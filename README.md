# ☀️ Solar POS

A desktop **Point-of-Sale, quoting and inventory management system** for a solar equipment business, built with **Java 21**, **Swing** and **MySQL**.

Solar POS handles the full sales cycle: selling hardware, tracking customers and outstanding balances, monitoring stock, and producing management reports such as the **Quarterly Hardware Turnover & Warranty Liability Report**.

---

## Table of Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [User Roles](#user-roles)
- [Quarterly Turnover Report](#quarterly-turnover-report)
- [Building a Runnable JAR](#building-a-runnable-jar)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Features

### For all users
- **Secure login** with BCrypt-hashed passwords and a modern animated login screen
- **Dashboard** with key figures and charts
- **New Sale (POS)** for creating sales orders and quotations
- **Customer management** to add, edit and search customers
- **Inventory** for solar hardware (panels, inverters, batteries and more), with stock levels, prices and warranty terms
- **Collect Balance** to record payments against outstanding customer balances

### Admin only
- **Sales History** to browse and review all past orders
- **Profit & Loss** reporting
- **Customer Analytics** to see buying behaviour per customer
- **Stock Valuation** showing the capital tied up in inventory
- **User Management** to create and manage staff accounts and roles
- **Reports** to generate the Quarterly Hardware Turnover & Warranty Liability Report, view it on screen and export it to **PDF**

### General
- Modern look and feel using FlatLaf
- Role-based sidebar: cashiers never see admin screens
- Charts powered by JFreeChart
- Excel export support through Apache POI

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Build tool | Maven |
| UI | Java Swing + [FlatLaf](https://www.formdev.com/flatlaf/) 3.4.1 |
| Database | MySQL (via `mysql-connector-j` 8.4.0) |
| Reporting | [JasperReports](https://community.jaspersoft.com/) 7.x (report designed in Jaspersoft Studio 7) |
| Charts | JFreeChart 1.5.4 |
| Security | jBCrypt 0.4 (password hashing) |
| Excel export | Apache POI 5.2.5 |
| IDE | Apache NetBeans (any Maven-capable IDE works) |

---

## Project Structure

```
SolarPOS/
├── pom.xml
├── README.md
├── database/                      # (recommended) schema.sql for setting up the DB
├── docs/screenshots/              # README images
└── src/
    └── main/
        ├── java/com/mycompany/solarpos/
        │   ├── SolarPOS.java      # Application entry point
        │   ├── dao/               # Data access classes (UserDAO, ...)
        │   ├── db/                # DBConnection
        │   ├── model/             # Entities (User, ...)
        │   ├── ui/                # Swing screens (LoginForm, MainFrame, ReportPanel, ...)
        │   └── util/              # Session, Validator, ValidationException, ...
        └── resources/
            └── reports/
                └── turnover.jrxml # JasperReports template
```

---

## Getting Started

### Prerequisites

- **JDK 21** or newer
- **Apache Maven 3.9+** (bundled with NetBeans)
- **MySQL 8.x** (or a compatible MariaDB)
- Git

Check your versions:

```bash
java -version
mvn -version
mysql --version
```

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/SolarPOS.git
cd SolarPOS
```

### 2. Create the database

Log in to MySQL and create an empty database:

```sql
CREATE DATABASE solarpos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then import the schema (if you have exported one):

```bash
mysql -u root -p solarpos_db < database/schema.sql
```

### 3. Configure the database connection

Open `src/main/java/com/mycompany/solarpos/db/DBConnection.java` and set your own values:

```java
private static final String URL  = "jdbc:mysql://localhost:3306/solarpos_db";
private static final String USER = "your_mysql_user";
private static final String PASS = "your_mysql_password";
```

> ⚠️ **Never commit real credentials** to a public repository. Keep the repo private, or move the settings into a config file that is listed in `.gitignore`.

---

## Database Setup

The application expects these core tables. Column names below are the ones used by the turnover report; your full schema may contain more.

| Table | Purpose | Key columns used by reports |
|---|---|---|
| `inventory_hardware` | Solar hardware catalogue and stock | `item_id`, `item_code`, `brand`, `model`, `category`, `stock_qty`, `unit_price`, `warranty_months`, `date_received` |
| `sales_orders` | One row per sale or quotation | `order_id`, `customer_id`, `order_date` |
| `sale_details` | Line items of each order | `order_id`, `item_id`, `qty` |
| `customers` | Customer records | `customer_id` |
| `users` | Staff accounts (BCrypt password hash and role) | username, password hash, role |

---

## Running the Application

### From NetBeans
1. Open the project (**File → Open Project**).
2. Right-click the project and choose **Clean and Build**.
3. Right-click the project and choose **Run**.

### From the command line

```bash
mvn clean package
java -jar target/SolarPOS-1.0-SNAPSHOT.jar
```

Log in with an existing user account. If you have no users yet, insert an admin user into the `users` table with a BCrypt-hashed password.

---

## User Roles

| Role | Access |
|---|---|
| **Admin** | Everything: all cashier screens plus Sales History, Profit & Loss, Analytics, Stock Valuation, User Management and Reports |
| **Cashier** | Dashboard, New Sale, Customers, Inventory and Collect Balance |

Admin-only screens are not even created for cashier sessions.

---

## Quarterly Turnover Report

The **Quarterly Hardware Turnover & Warranty Liability Report** helps management decide which stock to discount, bundle or return to the supplier before warranty coverage runs out.

It joins four tables: `inventory_hardware`, `sale_details`, `sales_orders` and `customers`.

### Metrics

| Metric | Formula |
|---|---|
| **Turnover ratio** | `units sold ÷ (stock on hand + units sold)` |
| **Capital tied up** | `stock quantity × unit price` |
| **Supplier warranty end** | `date received + warranty months` |
| **Days in stock** | `today − date received` |

### Status rule

An item is flagged **SLOW-MOVING** when:

- its turnover ratio is **below 0.25**, **or**
- its supplier warranty ends **within 6 months** while it still has stock.

Otherwise it is marked **OK**.

### Using the report
1. Open **Reports** in the sidebar (admin only).
2. Pick a **Year** and **Quarter**.
3. Click **Generate & View** to open the report viewer.
4. Click **Export to PDF** to save a copy.

### Editing the template
The template lives at `src/main/resources/reports/turnover.jrxml` and can be edited in **Jaspersoft Studio 7.x**. The app compiles the copy in `src/main/resources`, so always save your changes there and rebuild.

---

## Building a Runnable JAR

The `maven-shade-plugin` packages the app and all dependencies into one executable JAR:

```bash
mvn clean package
```

Output: `target/SolarPOS-1.0-SNAPSHOT.jar`

Run it with:

```bash
java -jar target/SolarPOS-1.0-SNAPSHOT.jar
```

The shade plugin configuration includes an `AppendingTransformer` for `jasperreports_extension.properties`. Keep it, because JasperReports needs the merged file to work from a single JAR.

---

## Troubleshooting

| Problem | Cause and fix |
|---|---|
| `Attribute 'uuid' is not allowed in element 'jasperReport'` | The report was saved by Jaspersoft Studio 7 but the project uses JasperReports 6. Use JasperReports **7.x** in `pom.xml`. |
| `cannot find symbol: JRPdfExporter` | In JasperReports 7 the PDF classes moved. Import `net.sf.jasperreports.pdf.JRPdfExporter` and `SimplePdfExporterConfiguration`, and include the `jasperreports-pdf` dependency. |
| `The method YEAR(Integer) is undefined` | A text field expression is missing quotes. Use `"Year " + $P{P_YEAR}` instead of `Year + $P{P_YEAR}`. |
| Report changes not showing | The app loads `src/main/resources/reports/turnover.jrxml`. Edit that copy, then Clean and Build. |
| Report shows no data | There are no sales in the chosen quarter. Try another year or quarter. |
| Empty boxes (▯) instead of icons | The Unicode escape is malformed, or the font lacks the glyph. Use full code points (for example `Character.toChars(0x1F4E6)`) or set the font to `Segoe UI Emoji`. |
| Cannot connect to the database | Check that MySQL is running and that `DBConnection.java` has the right URL, user and password. |

---

## Roadmap

- [ ] Quotation PDF export from the POS screen
- [ ] Low-stock alerts on the dashboard
- [ ] Excel export for every report
- [ ] Customer SMS and email receipts
- [ ] Backup and restore tools

---

## Contributing

Contributions, issues and feature requests are welcome.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push the branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

Choose a license for your project and add a `LICENSE` file. If you are unsure, the [MIT License](https://choosealicense.com/licenses/mit/) is a common option for open-source projects. Remove this section if the project is private.

---

## Author

**Your Name**
GitHub: Kalpa-Mihiranga
