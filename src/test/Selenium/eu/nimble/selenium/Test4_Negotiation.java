package eu.nimble.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test4_Negotiation implements SeleniumInterface {

    @Override
    public void execute() throws Exception {
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

        // Go to search page
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuSearch\"]"))).click();
        driver.findElement(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[1]/li[3]/div/a[1]")).click();

        // Search for QuantumExampleProduct
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"searchTerm\"]"))).sendKeys("QuantumExampleProduct");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search/simple-search-form/form/div[1]/span/button"))).click();

        // Click (First one)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search/simple-search-form/form/div[3]/div[1]/div[2]/div[2]/div[1]/div/div"))).click();

        // Negotiation
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/h3/button"))).click();
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/ul/li[2]/a")).click();
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/ul/li[3]/a")).click();

        // Fill amount
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/div[1]/amount-view/form/div/div/input[1]")).sendKeys("100");
        // Fill Price Base Quantity
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/div[1]/quantity-view[1]/form/div/div/div/input[1]")).sendKeys("50");
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/div[1]/quantity-view[1]/form/div/div/div/input[2]")).sendKeys("EUR");
        // Fill Requested Quantity
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/div[1]/quantity-view[2]/form/div/div/div/input[1]")).sendKeys("100");
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/div[1]/quantity-view[2]/form/div/div/div/input[2]")).sendKeys("EUR");
        // Fill Requested Delivery Period
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/div[2]/quantity-view/form/div/div/div/input[1]")).sendKeys("250");
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/div[2]/quantity-view/form/div/div/div/input[2]")).sendKeys("Days");
        // Fill Validity Period
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/quantity-view/form/div/div/div/input[1]")).sendKeys("10");
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/trading-details/quantity-view/form/div/div/div/input[2]")).sendKeys("Weeks");
        // Send a note
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/value-view/div/input")).sendKeys("Hello");

        // Send the request
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/negotiation/request-for-quotation/div/button")).click();

        // Check whether it is sent
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nimble-dashboard/ul/li[2]/a"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuBuyer\"]")));

        // Logout
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        driver.findElement(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[3]/li/div/a[3]")).click();


    }
}
