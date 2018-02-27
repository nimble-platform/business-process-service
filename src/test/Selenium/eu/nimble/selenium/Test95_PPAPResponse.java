package eu.nimble.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test95_PPAPResponse implements SeleniumInterface{
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
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuSeller\"]"))).click();
        // Go to business history
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nimble-dashboard/div[2]/div/div/table/tbody/tr[2]/td[8]/div/div/div[1]"))).click();

        // PPAP Response
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/div/ul/li[3]/a"))).click();

        // Path to the directory
        String path = System.getProperty("user.dir");

        // Add Design Documentation
        // Path to document
        String pathToDD = path + "/src/test/resources/DesignDocumentation.jpeg";

        WebElement chooseFile = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/ppap-document-upload/div[1]/div/div/table/tbody/tr[2]/td[2]/div/input")));
        String js = "arguments[0].style.height='auto'; arguments[0].style.visibility='visible';";
        ((JavascriptExecutor) driver).executeScript(js,chooseFile);

        chooseFile.sendKeys(pathToDD);

        // Add AppearanceApprovalReport
        // Path to document
        String pathToAAR = path + "/src/test/resources/AppearanceApprovalReport.xls";

        chooseFile = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/ppap-document-upload/div[1]/div/div/table/tbody/tr[7]/td[2]/div/input")));
        ((JavascriptExecutor) driver).executeScript(js,chooseFile);

        chooseFile.sendKeys(pathToAAR);

        // Add PartSubmissionWarrant.doc
        String pathToPSW = path + "/src/test/resources/PartSubmissionWarrant.doc";

        chooseFile = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/ppap-document-upload/div[1]/div/div/table/tbody/tr[9]/td[2]/div/input")));
        ((JavascriptExecutor) driver).executeScript(js,chooseFile);

        chooseFile.sendKeys(pathToPSW);

        // Send
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/product-bp-options/ppap/ppap-document-upload/div[2]/button"))).click();

        // Check whether it is sent
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"bpOptionsMenuSeller\"]")));

        // Logout
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        driver.findElement(By.xpath("/html/body/div/nimble-app/nav/div/ul[2]/li/div/a[3]")).click();

    }
}
