package isuconp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }

  @RestController
  class MyController {
    private MyService myService;

    public MyController(MyService myService) {
      this.myService = myService;
    }

    @GetMapping("/")
    String home() {
      Map<String, Object> user = myService.login("kitty", "kittykitty");
      return user.toString();
    }

    @GetMapping("/initialize")
    void initialize() {
      myService.initialize();
    }
  }

  @Service
  class MyService {
    private Logger logger = LoggerFactory.getLogger(MyService.class);

    private JdbcTemplate jdbcTemplate;

    public MyService(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void initialize() {
      String[] sqls = {
          "DELETE FROM users WHERE id > 1000",
          "DELETE FROM posts WHERE id > 10000",
          "DELETE FROM comments WHERE id > 100000",
          "UPDATE users SET del_flg = 0",
          "UPDATE users SET del_flg = 1 WHERE id % 50 = 0"
      };
      for (String sql : sqls) {
        logger.info("SQL: {}", sql);
        jdbcTemplate.execute(sql);
      }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(String accountName, String password) {
      return tryLogin(accountName, password);
    }

    private Map<String, Object> tryLogin(String accountName, String password) {
      var user = jdbcTemplate.queryForMap(
          "SELECT * FROM users WHERE account_name = ? AND del_flg = 0", accountName);

      if (user != null && user.get("passhash").equals(calculatePasshash(accountName, password))) {
        return user;
      }
      return null;
    }

    private String digest(String src) {
      try {
        Process process = Runtime.getRuntime().exec(
            new String[] { "openssl", "dgst", "-sha512" });
        BufferedReader br = new BufferedReader(new InputStreamReader(
            process.getInputStream()));
        process.getOutputStream().write(src.getBytes());
        process.getOutputStream().close();

        String line = null;
        while ((line = br.readLine()) != null) {
          Pattern pattern = Pattern.compile("^.*= (.*)$");
          Matcher matcher = pattern.matcher(line);
          if (matcher.matches()) {
            return matcher.group(1);
          }
        }
      } catch (IOException e) {
        logger.error("digest error: src={}", src, e);
        ;
      }
      return null;
    }

    private String calculateSalt(String accountName) {
      return digest(accountName);
    }

    private String calculatePasshash(String accountName, String password) {
      return digest(password + ":" + calculateSalt(accountName));
    }
  }
}
