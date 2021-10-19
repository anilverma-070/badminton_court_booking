package com.orienlabs.booking;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;

public class Main {
	static WebDriver w;
	static String bookingHomeUrl= System.getProperty("homePageUrl", "https://readinguniversity.leisurecloud.net/Connect/memberHomePage.aspx");
	static String loginId= System.getProperty("loginUser");// "narsingraoch@gmail.com";
	static String loginPwd= System.getProperty("loginPwd");//
	
	static String bookingCourt= System.getProperty("courtName", "Badminton Courts 5-8");
	static String bookingTimeSlot= System.getProperty("timeslot", "20:00");
	static int maxRetryDurationMinutes= Integer.parseInt(System.getProperty("maxRetryDuration", "5"));
	static int exitAfterBookingCount= Integer.parseInt(System.getProperty("exitAfterBookingCount", "1"));
	static int weeksFromNow= Integer.parseInt(System.getProperty("weeksFromNow", "1")); //0 means book for current week, 1 means next week
	
	static {
		//WebDriverManager.chromedriver().setup();
	}

	public static void main(String[] args) throws Exception {
		// Launch browser
		// Ensure browser closes on VM shutdown
		
		//WebDriver w = new ChromeDriver();
		w = new HtmlUnitDriver(BrowserVersion.BEST_SUPPORTED, true);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> w.quit()));
		
		w.navigate().to(bookingHomeUrl);
		w.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
		
		// Login
		//narsingraoch@gmail.com
		//
		w.findElement(By.id("ctl00_MainContent_InputLogin")).sendKeys(loginId);
		w.findElement(By.id("ctl00_MainContent_InputPassword")).sendKeys(loginPwd);
		w.findElement(By.id("ctl00_MainContent_btnLogin")).click();
		
		//w.getPageSource();
		
		int totalSlotsBooked= 0;
		
		goToCalender();
		
		long startTime= System.currentTimeMillis();
		while (true) {
			if(System.currentTimeMillis() - startTime > (maxRetryDurationMinutes * 60 * 1000))
			{
				System.out.println("Timeout of " + maxRetryDurationMinutes + " minute/s hit. Total " + totalSlotsBooked + " slots were booked.");
				System.exit(0);
			}
			
			//scan current week + next 2 weeks for any available slot
			int availableSlotsSize= w.findElements(By.xpath("//table[@id='slotsGrid']//tr/td[.='" + bookingTimeSlot + "']/..//input[@value='Available']")).size();
			System.out.println("Found slots in the week: " + availableSlotsSize);
				
			if (availableSlotsSize == 0)
			{
				System.out.println("No available slots found right now, trying again...");
				w.navigate().refresh();
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
					
					if(totalSlotsBooked >= exitAfterBookingCount)
						System.exit(0);
				}
			}
			
			goToCalender();
		}
	}
	
	public static void goToCalender()
	{
		// Badminton courts 5-8
		w.navigate().to(bookingHomeUrl);
		w.findElement(By.xpath("//a[.='" + bookingCourt + "']")).click();

		// select specific week from now
		for (int i = 0; i < weeksFromNow; i++) {
			w.findElement(By.xpath("//button[contains(@id,'dateForward')]")).click();
		}

		try {
			System.out.print("Looking for slots in the week of ");
			System.out.println(w.findElement(By.id("ctl00_MainContent_startDate")).getText());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
