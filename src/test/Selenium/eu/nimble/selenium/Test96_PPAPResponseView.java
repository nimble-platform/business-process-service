package eu.nimble.selenium;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test96_PPAPResponseView implements SeleniumInterface {
    @Override
    public void execute() {
        //Launch the website
        driver.get("http://localhost:9092/#/user-mgmt/login");

        // Get email and password input elements
        WebElement email = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"email\"]")));
        WebElement password = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"password\"]")));

        email.clear();
        email.sendKeys(emailAddressBuyer);

        password.clear();
        password.sendKeys(userPasswordBuyer);

        // Submit
        driver.findElement(By.xpath("/html/body/div[1]/nimble-app/nimble-login/credentials-form/form/button[1]")).click();

        // Wait until submit button is gone.In other words,wait until dashboard page is available.
        wait.until(ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("/html/body/div/nimble-app/nimble-login/credentials-form/form/button[1]"))));
        // Purchase
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nimble-dashboard/ul/li[2]/a"))).click();
        // Options
     //   wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuBuyer\"]"))).click();
        // Business History
    //    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nimble-dashboard/div[3]/div/div/table/tbody/tr[2]/td[8]/div/div/div"))).click();
        WebElement elem = driver.findElement(By.xpath("/html/body/div/nimble-app/nimble-dashboard/div[2]/div/div/table/tbody/tr[3]/td[8]/div/div/div/label"));
        String js = "arguments[0].click();";
        ((JavascriptExecutor) driver).executeScript(js, elem);
        // Check documents
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/ppap-document-download/div/div/div/table/tbody/tr[2]/td/button")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/ppap-document-download/div/div/div/table/tbody/tr[7]/td/button")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/ppap-document-download/div/div/div/table/tbody/tr[9]/td/button")));

        // Logout
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        driver.findElement(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[3]/li/div/a[3]")).click();

    }
}
