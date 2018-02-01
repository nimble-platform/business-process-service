package eu.nimble.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test92_TransportExecutionPlan implements SeleniumInterface {
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

        // Go to search page
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        driver.findElement(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[1]/li[3]/a")).click();

        // Search for QuantumExampleProduct
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"searchTerm\"]"))).sendKeys("QuantumExampleProductService");
        driver.findElement(By.xpath("/html/body/div/nimble-app/simple-search/simple-search-form/form/div[1]/span/button")).click();

        // Click (First one)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search/simple-search-form/form/div[3]/div[1]/div[2]/div[2]/div[1]/div/div"))).click();

        // Options
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/h3/button"))).click();
        // Transpor_Execution_Plan
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/ul/li[3]/a"))).click();

        // Fill GoodsItem
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[1]/value-view/div/input"))).sendKeys("QuantumExampleProduct");
        // Fill Gross Volume Measure
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[1]/quantity-view[1]/form/div/div/div/input[1]"))).sendKeys("1254");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[1]/quantity-view[1]/form/div/div/div/input[2]"))).sendKeys("Str");
        // Fill Gross Weight Measure
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[1]/quantity-view[2]/form/div/div/div/input[1]"))).sendKeys("44");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[1]/quantity-view[2]/form/div/div/div/input[2]"))).sendKeys("Str");
        // Fill From Location
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[2]/address-view[1]/div/input"))).sendKeys("London");
        // Fill To Location
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[2]/address-view[2]/div/input"))).sendKeys("Paris");
        // Use Service
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/simple-search-details/div/product-bp-options/transport-execution-plan-bp/div[2]/transport-execution-plan-request/div[3]/button"))).click();

        // Check whether the request is sent.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuBuyer\"]")));

        // Logout
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        driver.findElement(By.xpath("/html/body/div/nimble-app/nav/div/ul[2]/li/div/a[3]")).click();

    }
}
