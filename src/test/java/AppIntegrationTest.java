import jp.ac.osaka_u.ist.ipogBDD.Main;
import org.junit.jupiter.api.Test;

public class AppIntegrationTest {
    @Test
    public void testMain() {
        String path = "src/test/resources/inputsamples/";
        String[] args = {"-i", path + "simple00.txt"};
        Main.main(args);
    }
}
