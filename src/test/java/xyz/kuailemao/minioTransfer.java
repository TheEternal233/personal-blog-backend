package xyz.kuailemao;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;

import java.io.InputStream;

public class minioTransfer {
    public static void main(String[] args) throws Exception {
        // 线上源MinIO
        MinioClient sourceClient = MinioClient.builder()
                .endpoint("http://82.156.225.4:9000")
                .credentials("admin","4626463ljh.w")
                .build();
        // 本地目标MinIO
        MinioClient targetClient = MinioClient.builder()
                .endpoint("http://192.168.100.132:9000")
                .credentials("minioadmin","minioadmin")
                .build();

        String bucket = "blog";
        Iterable<Result<Item>> items = sourceClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> resultItem : items) {
            Item obj = resultItem.get();
            String objName = obj.objectName();

            // 跳过文件夹（MinIO里目录对象不需要上传）
            if (obj.isDir()){
                System.out.println("跳过目录：" + objName);
                continue;
            }

            try (InputStream stream = sourceClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objName)
                    .build())) {

                targetClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objName)
                        .stream(stream, -1, 5*1024*1024)
                        .build());

                System.out.println("迁移成功：" + objName);
            } catch (Exception e) {
                System.err.println("迁移失败：" + objName + " 异常："+e.getMessage());
            }
        }
        System.out.println("====全部任务执行完毕====");
    }
}