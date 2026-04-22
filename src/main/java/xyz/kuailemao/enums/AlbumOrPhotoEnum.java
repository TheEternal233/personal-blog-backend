package xyz.kuailemao.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author TheEternal
 * @since 2026/4/1
 * @description 相册枚举
 */
@Getter
@AllArgsConstructor
public enum AlbumOrPhotoEnum {
    ALBUM(1, "相册"),
    PHOTO(2, "照片");

    private final Integer code;
    private final String desc;
}
