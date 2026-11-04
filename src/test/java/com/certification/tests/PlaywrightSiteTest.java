package com.certification.tests;

import com.certification.base.BaseTest;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PlaywrightSiteTest extends BaseTest {

    @Test
    public void simpleFormDemoDisplaysEnteredMessage() {
        getPage().navigate("https://www.testmuai.com/selenium-playground/");
        getPage().getByText("Simple Form Demo").click();
        getPage().waitForLoadState(LoadState.NETWORKIDLE);
        Assert.assertTrue(getPage().url().contains("simple-form-demo"));

        String message = "Welcome to TestMu AI";
        getPage().locator("input#user-message").fill(message);
        getPage().locator("#showInput").click();

        PlaywrightAssertions.assertThat(getPage().locator("#message")).hasText(message);
    }

    @Test
    public void dragSliderToNinetyFive() {
        getPage().navigate("https://www.testmuai.com/selenium-playground/");
        getPage().getByText("Drag & Drop Sliders").click();
        getPage().waitForLoadState(LoadState.NETWORKIDLE);
        Assert.assertTrue(getPage().url().contains("drag-drop-range-sliders-demo"));

        int min = 1;
        int max = 100;
        int targetValue = 95;

        Locator slider = getPage().locator("#slider3 input[type='range']");
        Locator rangeOutput = getPage().locator("#rangeSuccess");
        BoundingBox box = slider.boundingBox();

        double targetX = box.x + box.width * ((double) (targetValue - min) / (max - min));
        double y = box.y + box.height / 2;

        Mouse mouse = getPage().mouse();
        mouse.move(box.x + box.width / 2, y);
        mouse.down();
        mouse.move(targetX, y, new Mouse.MoveOptions().setSteps(10));
        mouse.up();

        int currentValue = Integer.parseInt(rangeOutput.textContent());
        while (currentValue != targetValue) {
            slider.press(currentValue < targetValue ? "ArrowRight" : "ArrowLeft");
            currentValue = Integer.parseInt(rangeOutput.textContent());
        }

        PlaywrightAssertions.assertThat(rangeOutput).hasText(String.valueOf(targetValue));
    }

    @Test
    public void inputFormSubmitShowsValidationThenSuccess() {
        getPage().navigate("https://www.testmuai.com/selenium-playground/");
        getPage().getByText("Input Form Submit").click();
        getPage().waitForLoadState(LoadState.NETWORKIDLE);

        Locator submitButton = getPage().locator("#seleniumform button[type='submit']");
        submitButton.click();

        Locator nameField = getPage().locator("#name");
        String validationMessage = (String) nameField.evaluate("el => el.validationMessage");
        Assert.assertFalse(validationMessage.isEmpty(), "Expected a 'please fill in the fields' validation message");

        nameField.fill("John Doe");
        getPage().locator("#inputEmail4").fill("john.doe@example.com");
        getPage().locator("#inputPassword4").fill("P@ssword123");
        getPage().locator("#company").fill("Acme Corp");
        getPage().locator("#websitename").fill("https://acme.example.com");
        getPage().locator("select[name='country']").selectOption(new SelectOption().setLabel("United States"));
        getPage().locator("#inputCity").fill("San Francisco");
        getPage().locator("#inputAddress1").fill("123 Main St");
        getPage().locator("#inputAddress2").fill("Suite 100");
        getPage().locator("#inputState").fill("California");
        getPage().locator("#inputZip").fill("94104");

        submitButton.click();

        PlaywrightAssertions.assertThat(getPage().locator(".success-msg"))
                .hasText("Thanks for contacting us, we will get back to you shortly.");
    }
}

