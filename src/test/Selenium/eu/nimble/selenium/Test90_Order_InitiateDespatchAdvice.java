package eu.nimble.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test90_Order_InitiateDespatchAdvice implements SeleniumInterface {
    @Override
    public void execute() {
        //Launch the website
        driver.get("http://localhost:9092/#/user-mgmt/login");

        // Get email and password input elements
        WebElement email = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"email\"]")));
        WebElement password = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"password\"]")));

        email.clear();
        email.sendKeys(emailAddress);

        password.clear();
        password.sendKeys(userPassword);

        // Submit
        driver.findElement(By.xpath("/html/body/div[1]/nimble-app/nimble-login/credentials-form/form/button[1]")).click();

        // Wait until submit button is gone.In other words,wait until dashboard page is available.
        wait.until(ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("/html/body/div/nimble-app/nimble-login/credentials-form/form/button[1]"))));

        // Options
        WebElement elem = driver.findElement(By.xpath("/html/body/div/nimble-app/nimble-dashboard/div[2]/div/div/table/tbody/tr[3]/td[8]/div/div/div/label"));
        String js = "arguments[0].click();";
        ((JavascriptExecutor) driver).executeScript(js, elem);
        // Order Response Details
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/order-bp/div[1]/ul/li[4]/a"))).click();
        // Initiate Despatch Advice
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/order-bp/div[2]/order-response/div/button[2]"))).click();

        // Despatch Advice Details
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[1]/ul/li[3]/a"))).click();
        // Fill Delivered Quantity
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[2]/despatch-advice/quantity-view/form/div/div/div/input[1]"))).sendKeys("123");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[2]/despatch-advice/quantity-view/form/div/div/div/input[2]"))).sendKeys("pieces");
        // Fill Handling Instructions
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[2]/despatch-advice/shipment-view/value-view[1]/div/input"))).sendKeys("String");
        // Send
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[2]/despatch-advice/div/div/button"))).click();

        // Check whether it is sent
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuSeller\"]")));

        // Logout
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[3]/li/div/a[3]"))).click();
    }
}
