# Jmix 腾讯云对象存储

此扩展提供了一个 FileStorage 实现，可将基于Jmix文件引用(FileRef)数据存储到 [腾讯云对象存储](https://cloud.tencent.com/document/product/436) 。

## 安装

下表展示了扩展组件的版本以及兼容的Jmix平台版本。

| Jmix 版本     | 扩展组件版本     | Implementation                             |
|--------------|----------------|--------------------------------------------|
| 1.1.*        | 1.0.0          | cn.jmix:tencentfs-starter:1.0.0                |

`build.gradle` 文件中添加依赖:

```gradle
implementation 'cn.jmix:tencentfs-starter:1.0.0'
```

# 配置
在 `application.properties` 配置文件中添加以下属性配置:

| 属性名                      | 默认值   | 说明                                                                                                          |
|----------------------------|---------|----------------------------------------|
| jmix.tencentfs.secretId    |         | secretId 和 secretKey 合称为云 API 密钥  |        |         
| jmix.tencentfs.secretKey   |         | secretId 和 secretKey 合称为云 API 密钥  |
| jmix.tencentfs.bucket      |         | 存储桶                                  |
| jmix.tencentfs.region      |         | 地域                                    |
| jmix.tencentfs.chunkSize   |   8192  | 每个分片的大小，单位是KB                   |
| jmix.tencentfs.endpointUrl |         | 访问域名                                 |

