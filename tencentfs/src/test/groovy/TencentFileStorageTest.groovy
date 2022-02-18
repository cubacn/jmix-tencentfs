import cn.jmix.tencentfs.TencentFileStorageConfiguration
import io.jmix.core.CoreConfiguration
import io.jmix.core.FileRef
import io.jmix.core.FileStorage
import io.jmix.core.UuidProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import test_support.TencentFileStorageTestConfiguration
import test_support.TestContextInititalizer

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ContextConfiguration(
        classes = [CoreConfiguration, TencentFileStorageConfiguration,TencentFileStorageTestConfiguration],
        initializers = [TestContextInititalizer]
)
class TencentFileStorageTest extends Specification {

    @Autowired
    private FileStorage fileStorage

    def "save stream"(){
        def fileName=UuidProvider.createUuid().toString()+".txt";
        def fileStream=this.getClass().getClassLoader().getResourceAsStream("files/simple.txt");
        def fileRef=fileStorage.saveStream(fileName,fileStream);
        def openedStream=fileStorage.openStream(fileRef);
        expect:
            openedStream!=null
    }

    def "fileExists"() {
        def storageName = fileStorage.getStorageName()
        def fileKey = "2021/11/09/e24e6c35-767d-741b-0edb-ae54a34881b6.txt"
        def fileName="e24e6c35-767d-741b-0edb-ae54a34881b6.txt"

        def fileref = new FileRef(storageName, fileKey, fileName);
        def exists = fileStorage.fileExists(fileref)

        expect:  exists

    }


    def "removeFile"(){
        def storageName = fileStorage.getStorageName()
        def fileKey = "2021/11/09/e24e6c35-767d-741b-0edb-ae54a34881b6.txt"
        def fileName="e24e6c35-767d-741b-0edb-ae54a34881b6.txt"

        def fileref = new FileRef(storageName, fileKey, fileName);
        fileStorage.removeFile(fileref)


        def exists = fileStorage.fileExists(fileref)

        expect:  !exists
    }


    def "Tencent storage initialized"() {
        expect:
        fileStorage.getStorageName() == TencentFileStorage.DEFAULT_STORAGE_NAME
    }
}