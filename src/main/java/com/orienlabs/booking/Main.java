package com.orienlabs.booking;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
//import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

public class Main {
	
	static {
		WebDriverManager.chromedriver().setup();
	}

	public static void main(String[] args) throws Exception {
		// Launch browser
		// Ensure browser closes on VM shutdown
		String bookingHomeUrl= System.getProperty("homePageUrl", "https://readinguniversity.leisurecloud.net/Connect/memberHomePage.aspx");
		String loginId= System.getProperty("loginUser");// "narsingraoch@gmail.com";
		String loginPwd= System.getProperty("loginPwd");//
		
		String bookingCourt= System.getProperty("courtName", "Badminton Courts 5-8");
		String bookingTimeSlot= System.getProperty("timeslot", "07:00");
		int maxRetryDurationMinutes= Integer.parseInt(System.getProperty("maxRetryDuration", "30"));
		
		//WebDriver w = new ChromeDriver();
		WebDriver w = new HtmlUnitDriver(BrowserVersion.BEST_SUPPORTED, true);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> w.quit()));
		
		w.navigate().to(bookingHomeUrl);
		w.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
		
		// Login
		//narsingraoch@gmail.com
		//
		w.findElement(By.id("ctl00_MainContent_InputLogin")).sendKeys(loginId);
		w.findElement(By.id("ctl00_MainContent_InputPassword")).sendKeys(loginPwd);
		w.findElement(By.id("ctl00_MainContent_btnLogin")).click();
		
		//w.getPageSource();
		
		int totalSlotsBooked= 0;
		
		//book for current and next week, keep a single loop to make upto 10 bookings
		long startTime= System.currentTimeMillis();
		while (true) {
			
			if(System.currentTimeMillis() - startTime > (maxRetryDurationMinutes * 60 * 1000))
			{
				System.out.println("Timeout of " + maxRetryDurationMinutes + " minute/s hit. Total " + totalSlotsBooked + " slots were booked.");
				System.exit(0);
			}
			
			//Badminton courts 5-8
			w.navigate().to(bookingHomeUrl);
			w.findElement(By.xpath("//a[.='" + bookingCourt + "']")).click();
			
			//scan current week + next 2 weeks for any available slot
			int availableSlotsSize= 0;
			for (int j = 0; j < 2; j++) {
				try {
					System.out.print("Looking for slots in the week of ");
					System.out.println(w.findElement(By.id("ctl00_MainContent_startDate")).getText());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				availableSlotsSize= w.findElements(By.xpath("//table[@id='slotsGrid']//tr/td[.='" + bookingTimeSlot + "']/..//input[@value='Available']")).size();
				System.out.println("Found slots in the week: " + availableSlotsSize);
				
				if(availableSlotsSize > 0)
					break; //go booking :)
				else
				{
					System.out.println("Skipping to next week...");
					w.findElement(By.xpath("//button[contains(@id,'dateForward')]")).click();
				}
			}
			
			if (availableSlotsSize == 0)
			{
				System.out.println("No available slots found right now, trying again...");
				continue;
			}
			
			//check if slot is still available
			List<WebElement> availableSlots = w.findElements(By.xpath("//table[@id='slotsGrid']//tr/td[.='" + bookingTimeSlot + "']/..//input[@value='Available']"));
			if (availableSlots.size() > 0) {
				availableSlots.get(0).click();

				// check if slots are still available
				List<WebElement> availableSlotsOnDay = w.findElements(By.xpath("//input[@value='" + bookingTimeSlot + "']"));
				if (availableSlotsOnDay.size() > 0) {
					// click on timeslot button to book
					availableSlotsOnDay.get(0).click();

					// confirm booking
					w.findElement(By.xpath("//input[@value='Book']")).click();
					totalSlotsBooked++;

					// print completed booking
					try {
						System.out.println(w.findElement(By.xpath("//h1/following-sibling::div")).getText());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
