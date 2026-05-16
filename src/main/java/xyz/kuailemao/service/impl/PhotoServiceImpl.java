package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import xyz.kuailemao.domain.dto.DeletePhotoOrAlbumDTO;
import xyz.kuailemao.domain.dto.PhotoAlbumDTO;
import xyz.kuailemao.domain.entity.Photo;
import xyz.kuailemao.domain.response.ResponseResult;
import xyz.kuailemao.domain.vo.PageVO;
import xyz.kuailemao.domain.vo.PhotoAndAlbumListVO;
import xyz.kuailemao.enums.AlbumOrPhotoEnum;
import xyz.kuailemao.enums.UploadEnum;
import xyz.kuailemao.exceptions.FileUploadException;
import xyz.kuailemao.mapper.PhotoMapper;
import xyz.kuailemao.service.PhotoService;
import xyz.kuailemao.utils.FileUploadUtils;
import xyz.kuailemao.utils.SecurityUtils;
import xyz.kuailemao.utils.StringUtils;

import java.util.List;
import java.util.Objects;

import static xyz.kuailemao.constants.SQLConst.LIMIT_ONE_SQL;
import static xyz.kuailemao.constants.SQLConst.ORDER_BY_CREATE_TIME_DESC;


/**
 * (Photo)表服务实现类
 *
 * @author kuailemao
 * @since 2025-01-16 16:33:08
 */
@Log4j2
@Service("photosService")
public class PhotoServiceImpl extends ServiceImpl<PhotoMapper, Photo> implements PhotoService {

    @Resource
    private PhotoMapper photoMapper;

    @Resource
    private FileUploadUtils fileUploadUtils;

    @Override
    public PageVO<List<PhotoAndAlbumListVO>> getBackPhotoList(Long pageNum, Long pageSize, Long parentId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null || userId == 0L) {
            return new PageVO<>(List.of(), 0L);
        }
        return getPhotoList(pageNum, pageSize, parentId, userId);
    }

    @Override
    public PageVO<List<PhotoAndAlbumListVO>> getFrontPhotoList(Long pageNum, Long pageSize, Long parentId) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null || userId == 0L) {
            return new PageVO<>(List.of(), 0L);
        }
        return getPhotoList(pageNum, pageSize, parentId, userId);
    }

    private PageVO<List<PhotoAndAlbumListVO>> getPhotoList(Long pageNum, Long pageSize, Long parentId, Long userId) {
        // 分页
        Page<Photo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Photo> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Photo::getUserId, userId);
        if (null != parentId) {
            lambdaQueryWrapper.eq(Photo::getParentId, parentId);
        } else {
            lambdaQueryWrapper.isNull(Photo::getParentId);
        }
        // 优先显示相册，再显示照片，时间倒序
        lambdaQueryWrapper.last(ORDER_BY_CREATE_TIME_DESC);
        photoMapper.selectPage(page, lambdaQueryWrapper);
        if (page.getRecords().isEmpty()) return new PageVO<>(List.of(), page.getTotal());
        // 查询每个相册的封面
        for (Photo photo : page.getRecords()) {
            if (Objects.equals(photo.getType(), AlbumOrPhotoEnum.ALBUM.getCode())) {
                LambdaQueryWrapper<Photo> coverWrapper = new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getParentId, photo.getId())
                        .eq(Photo::getType, AlbumOrPhotoEnum.PHOTO.getCode())
                        .eq(Photo::getUserId, userId)
                        .last(ORDER_BY_CREATE_TIME_DESC).last(LIMIT_ONE_SQL);
                Photo photoOne = photoMapper.selectOne(coverWrapper);
                if (null != photoOne && StringUtils.isValidUrl(photoOne.getUrl())) {
                    page.getRecords().get(page.getRecords().indexOf(photo)).setUrl(photoOne.getUrl());
                }else{
                    page.getRecords().get(page.getRecords().indexOf(photo)).setUrl("");
                }
            }
        }

        List<PhotoAndAlbumListVO> photoAndAlbumListVOS = page.getRecords().stream().map(photo -> photo.asViewObject(PhotoAndAlbumListVO.class)).toList();
        return new PageVO<>(photoAndAlbumListVOS, page.getTotal());
    }

    @Override
    public ResponseResult<Void> createAlbum(PhotoAlbumDTO albumDTO) {
        Long currentUserId = SecurityUtils.getUserId();
        // ====================== 【修复重名】按当前用户作用域校验 ======================
        LambdaQueryWrapper<Photo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Photo::getName, albumDTO.getName());
        queryWrapper.eq(Photo::getUserId, currentUserId);

        if (albumDTO.getParentId() == null) {
            queryWrapper.isNull(Photo::getParentId);
        } else {
            queryWrapper.eq(Photo::getParentId, albumDTO.getParentId());
        }

        if (photoMapper.selectCount(queryWrapper) > 0) {
            return ResponseResult.failure("该名称已存在，请修改名称");
        }
        // ======================================================

        String albumUrl = "";
        if (albumDTO.getParentId() != null && photoMapper.selectCount(new LambdaQueryWrapper<Photo>().eq(Photo::getId, albumDTO.getParentId()).eq(Photo::getUserId, currentUserId)) > 0) {
            albumUrl = photoMapper.selectOne(new LambdaQueryWrapper<Photo>().eq(Photo::getId, albumDTO.getParentId()).eq(Photo::getUserId, currentUserId)).getUrl();
        }
        if (photoMapper.insert(Photo.builder()
                .userId(currentUserId)
                .parentId(albumDTO.getParentId())
                .name(albumDTO.getName())
                .description(albumDTO.getDescription())
                .type(AlbumOrPhotoEnum.ALBUM.getCode())
                .url(albumUrl + "/" + albumDTO.getName())
                .build()
        ) > 0) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Transactional
    @Override
    public ResponseResult<Void> uploadPhoto(MultipartFile file, String name, Long parentId) {
        try {
            if (file.isEmpty()) {
                return ResponseResult.failure("上传文件不能为空");
            }

            Long currentUserId = SecurityUtils.getUserId();

            // ====================== 修复重名：按当前用户作用域校验 ======================
            LambdaQueryWrapper<Photo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Photo::getName, name);
            queryWrapper.eq(Photo::getUserId, currentUserId);
            if (parentId == null) {
                queryWrapper.isNull(Photo::getParentId);
            } else {
                queryWrapper.eq(Photo::getParentId, parentId);
            }
            if (photoMapper.selectCount(queryWrapper) > 0) {
                return ResponseResult.failure("该名称已存在，请修改名称");
            }

            String bannerUrl;
            if (StringUtils.isNotNull(parentId)) {
                // 校验父相册属于当前用户
                Photo parentAlbum = photoMapper.selectOne(new LambdaQueryWrapper<Photo>()
                        .eq(Photo::getId, parentId)
                        .eq(Photo::getUserId, currentUserId));
                if (parentAlbum == null) {
                    return ResponseResult.failure("父相册不存在或无权限");
                }
                bannerUrl = parentAlbum.getUrl().replaceFirst("^/", "");
                bannerUrl = fileUploadUtils.upload(UploadEnum.PHOTO_ALBUM, file, name, bannerUrl);
            } else {
                bannerUrl = fileUploadUtils.upload(UploadEnum.PHOTO_ALBUM, file, name);
            }

            photoMapper.insert(Photo.builder()
                    .userId(currentUserId)
                    .parentId(parentId)
                    .name(name)
                    .url(bannerUrl)
                    .type(AlbumOrPhotoEnum.PHOTO.getCode())
                    .size(fileUploadUtils.convertFileSizeToMB(file.getSize()))
                    .build());

            return ResponseResult.success();
        } catch (FileUploadException e) {
            log.error("{}上传失败", UploadEnum.PHOTO_ALBUM.getDescription(), e);
            return ResponseResult.failure(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseResult<Void> updateAlbum(PhotoAlbumDTO albumDTO) {
        Long currentUserId = SecurityUtils.getUserId();
        // 校验所有权：该相册属于当前用户
        Photo existingPhoto = photoMapper.selectById(albumDTO.getId());
        if (existingPhoto == null || !existingPhoto.getUserId().equals(currentUserId)) {
            return ResponseResult.failure("无权限修改该相册");
        }
        if (
                photoMapper.updateById(Photo.builder()
                        .id(albumDTO.getId())
                        .name(albumDTO.getName())
                        .description(albumDTO.getDescription())
                        .build()
                ) > 0) {
            return ResponseResult.success();
        }
        return ResponseResult.failure();
    }

    @Transactional
    @Override
    public ResponseResult<Void> deletePhotoOrAlbum(DeletePhotoOrAlbumDTO deletePhotoOrAlbum) {
        Long currentUserId = SecurityUtils.getUserId();
        // 校验所有权
        Photo existingPhoto = photoMapper.selectById(deletePhotoOrAlbum.getId());
        if (existingPhoto == null || !existingPhoto.getUserId().equals(currentUserId)) {
            return ResponseResult.failure("无权限删除该相册或照片");
        }

        if (Objects.equals(deletePhotoOrAlbum.getType(), AlbumOrPhotoEnum.ALBUM.getCode())) {
            // 是否存在子相册
            if (photoMapper.selectCount(new LambdaQueryWrapper<Photo>().eq(Photo::getParentId, deletePhotoOrAlbum.getId())) > 0) {
                return ResponseResult.failure("删除失败，该相册下存在子相册或照片");
            }
            // 删除相册
            if (photoMapper.deleteById(deletePhotoOrAlbum.getId()) > 0) {
                return ResponseResult.success();
            }
            return ResponseResult.failure();
        } else {
            // 查询照片名称
            Photo photo = existingPhoto;
            // 查询父相册
            if (StringUtils.isNotNull(photo.getParentId())) {
                Photo album = photoMapper.selectById(deletePhotoOrAlbum.getParentId());
                fileUploadUtils.deleteFile(UploadEnum.PHOTO_ALBUM.getDir() + album.getName() + "/", fileUploadUtils.getFileName(photo.getUrl()));
            } else {
                fileUploadUtils.deleteFile(UploadEnum.PHOTO_ALBUM.getDir(), fileUploadUtils.getFileName(photo.getUrl()));
            }
            if (photoMapper.deleteById(deletePhotoOrAlbum.getId()) > 0) {
                return ResponseResult.success();
            }
            return ResponseResult.failure();
        }
    }
}
