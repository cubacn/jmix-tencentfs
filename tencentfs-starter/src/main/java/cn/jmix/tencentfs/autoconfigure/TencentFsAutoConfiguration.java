package cn.jmix.tencentfs.autoconfigure;

import cn.jmix.tencentfs.TencentFileStorageConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({TencentFileStorageConfiguration.class})
public class TencentFsAutoConfiguration {
}

