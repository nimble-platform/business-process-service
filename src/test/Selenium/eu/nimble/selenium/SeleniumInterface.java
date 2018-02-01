package eu.nimble.selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public interface SeleniumInterface {
    String emailAddress = "can@gmail.com";
    String userPassword = "123456";

    String emailAddressBuyer = "cav@gmail.com";
    String userPasswordBuyer = "123456";


    // Create a new instance of the Chrome driver
    WebDriver driver = new ChromeDriver();

    WebDriverWait wait = new WebDriverWait(driver,60);

    void execute() throws Exception;
}
