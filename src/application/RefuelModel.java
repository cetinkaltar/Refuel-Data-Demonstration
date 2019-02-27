package application;

public class RefuelModel {

	private String name;

	public String getName() {
		return name;
	}

	private double price;

	public double getPrice() {
		return price;
	}

	public void addPrice(double newPrice) {
		price = price + newPrice;
	}

	private double amount;

	public double getAmount() {
		return amount;
	}

	public void addAmount(double newAmount) {
		amount = amount + newAmount;
	}

	private int month;

	public int getMonth() {
		return month;
	}

	public RefuelModel(String name, double price, double amount, int month) {
		this.name = name;
		this.price = price;
		this.amount = amount;
		this.month = month;
	}
}
