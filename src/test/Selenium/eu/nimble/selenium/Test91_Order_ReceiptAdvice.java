package eu.nimble.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test91_Order_ReceiptAdvice implements SeleniumInterface {
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
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuBuyer\"]"))).click();
        // Business History
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nimble-dashboard/div[2]/div/div/table/tbody/tr[2]/td[8]/div/div/div[1]/label"))).click();
        // Receipt Advice Details
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[1]/ul/li[4]/a"))).click();
        // Fill Rejected Quantity
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[2]/receipt-advice/quantity-view/form/div/div/div/input[1]"))).sendKeys("135");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[2]/receipt-advice/quantity-view/form/div/div/div/input[2]"))).sendKeys("pieces");
        // Send
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/fulfilment/div[2]/receipt-advice/div/div/button"))).click();

        // Check whether it is sent
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nimble-dashboard/ul/li[2]/a"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuBuyer\"]")));

        // Logout
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        driver.findElement(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[3]/li/div/a[3]")).click();

    }
}
