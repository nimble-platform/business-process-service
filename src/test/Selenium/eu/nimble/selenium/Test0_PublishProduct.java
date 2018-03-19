package eu.nimble.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test0_PublishProduct implements SeleniumInterface {

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



        // Go to publish page
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[1]/li[2]/a"))).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/category-search/div/div[2]/button"))).click();

        // Select Single Product Tab
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"singleUpload\"]"))).click();

        // Set product name,description
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/catalogue-line-header/div/div[2]/value-view[2]/div/input")).sendKeys("QuantumExampleProduct");
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/catalogue-line-header/div/div[2]/value-view[3]/div/textarea")).sendKeys("QuantumExampleProductDescription");

        // Set price amount and price base quantity
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/ul/li[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/product-trading-details/amount-view/form/div/div/input[1]"))).sendKeys("1000");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/product-trading-details/amount-view/form/div/div/input[2]"))).sendKeys("EUR");
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/product-trading-details/quantity-view[1]/form/div/div/div/input[1]")).sendKeys("520");
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/product-trading-details/quantity-view[1]/form/div/div/div/input[2]")).sendKeys("EUR");

        // Add a category
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/div/div[1]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/category-search/div/div[1]/form/input"))).sendKeys("Bath");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div/nimble-app/category-search/div/div[1]/form/button"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/category-search/div/ul/li[1]"))).click();

        // Publish
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div/nimble-app/product-publish/div/div/button"))).click();

        // Check whether it is published.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/catalogue-view/div[1]/button")));

        // Logout
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        driver.findElement(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[3]/li/div/a[3]")).click();
    }
}
