package eu.nimble.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test1_PublishService implements SeleniumInterface {
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

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/category-search/div[2]/button"))).click();

        // Select Single Product Tab
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"singleUpload\"]"))).click();

        // Set product name,description
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/catalogue-line-header/div/div[2]/value-view[2]/div/input")).sendKeys("QuantumExampleProductService");
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/catalogue-line-header/div/div[2]/value-view[3]/div/textarea")).sendKeys("QuantumExampleProductServiceDescription");

        // Switch to logistic view
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/div/button"))).click();
        // Fill Service Type
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/transportation-service-details/div[1]/value-view[1]/div/input"))).sendKeys("String");
        // Fill Total Capacity
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/transportation-service-details/div[1]/quantity-view[1]/form/div/div/div/input[1]")).sendKeys("1223");
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/transportation-service-details/div[1]/quantity-view[1]/form/div/div/div/input[2]")).sendKeys("pieces");
        // Fill Estimated Duration
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/transportation-service-details/div[1]/quantity-view[2]/form/div/div/div/input[1]")).sendKeys("3");
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/transportation-service-details/div[1]/quantity-view[2]/form/div/div/div/input[2]")).sendKeys("weeks");
        // Fill Value Measure
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/transportation-service-details/div[4]/quantity-view/form/div/div/div/input[1]")).sendKeys("5");
        driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/transportation-service-details/div[4]/quantity-view/form/div/div/div/input[2]")).sendKeys("Str");
        // Publish
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-publish/div/div/button"))).click();
        // Check whether it is published.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/catalogue-view/div[1]/button")));

        // Logout
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[2]/li/div/a[3]"))).click();

    }
}
