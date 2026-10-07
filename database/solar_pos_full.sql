CREATE DATABASE  IF NOT EXISTS `solar_pos` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `solar_pos`;
-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: localhost    Database: solar_pos
-- ------------------------------------------------------
-- Server version	5.7.44-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `balance_payments`
--

DROP TABLE IF EXISTS `balance_payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `balance_payments` (
  `payment_id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `payment_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `collected_by` int(11) NOT NULL,
  `notes` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `order_id` (`order_id`),
  KEY `collected_by` (`collected_by`),
  CONSTRAINT `balance_payments_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `sales_orders` (`order_id`),
  CONSTRAINT `balance_payments_ibfk_2` FOREIGN KEY (`collected_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `balance_payments`
--

LOCK TABLES `balance_payments` WRITE;
/*!40000 ALTER TABLE `balance_payments` DISABLE KEYS */;
INSERT INTO `balance_payments` VALUES (1,9,1000740.30,'2026-09-30 11:17:34',1,'paid'),(2,2,405028.48,'2026-09-30 11:18:15',1,'given half');
/*!40000 ALTER TABLE `balance_payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `contractors`
--

DROP TABLE IF EXISTS `contractors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contractors` (
  `contractor_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `license_no` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`contractor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `contractors`
--

LOCK TABLES `contractors` WRITE;
/*!40000 ALTER TABLE `contractors` DISABLE KEYS */;
INSERT INTO `contractors` VALUES (1,'Solar Tech Installers','0771112233','LIC-ST-1021'),(2,'BrightWatt Engineering','0722223344','LIC-BW-2045'),(3,'EcoGrid Solutions','0713334455','LIC-EG-3078'),(4,'PowerPlus Contractors','0754445566','LIC-PP-4012');
/*!40000 ALTER TABLE `contractors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customers` (
  `customer_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `type` enum('RESIDENTIAL','COMMERCIAL') NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `address` varchar(255) NOT NULL,
  `grid_phase` enum('SINGLE','THREE') NOT NULL,
  `contractor_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`customer_id`),
  KEY `contractor_id` (`contractor_id`),
  CONSTRAINT `customers_ibfk_1` FOREIGN KEY (`contractor_id`) REFERENCES `contractors` (`contractor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers`
--

LOCK TABLES `customers` WRITE;
/*!40000 ALTER TABLE `customers` DISABLE KEYS */;
INSERT INTO `customers` VALUES (1,'Nimal Perera','RESIDENTIAL','0771234567','nimal.perera@example.com','25 Beach Road, Negombo','SINGLE',1),(2,'Sanduni Fernando','RESIDENTIAL','0712345678','sanduni.f@example.com','14 Flower Lane, Colombo 07','SINGLE',2),(3,'Kasun Silva','RESIDENTIAL','0759876543','kasun.silva@example.com','88 Temple Road, Kandy','THREE',1),(4,'Lanka Textiles (Pvt) Ltd','COMMERCIAL','0112345678','info@lankatextiles.example','Industrial Zone, Ja-Ela','THREE',3),(5,'Dilani Jayawardena','RESIDENTIAL','0704455667','dilani.j@example.com','7 Fort Road, Galle','SINGLE',2),(6,'Green Leaf Hotel','COMMERCIAL','0912233445','stay@greenleaf.example','Beach Drive, Unawatuna','THREE',3),(7,'Ruwan Bandara','RESIDENTIAL','0783322110','ruwan.b@example.com','52 Main Street, Kurunegala','SINGLE',4),(8,'Tharushi Wickramasinghe','RESIDENTIAL','0765566778','tharushi.w@example.com','19 Lake View, Gampaha','THREE',1),(9,'Ceylon Cold Storage','COMMERCIAL','0314455667','ops@ceyloncold.example','Airport Road, Katunayake','THREE',3),(10,'Sunrise Apparel Pvt Ltd','COMMERCIAL','0332211009','admin@sunriseapparel.example','Export Processing Zone, Gampaha','THREE',4);
/*!40000 ALTER TABLE `customers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_hardware`
--

DROP TABLE IF EXISTS `inventory_hardware`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_hardware` (
  `item_id` int(11) NOT NULL AUTO_INCREMENT,
  `item_code` varchar(30) NOT NULL,
  `category` enum('PANEL','INVERTER','BATTERY','KIT') NOT NULL,
  `brand` varchar(50) NOT NULL,
  `model` varchar(80) DEFAULT NULL,
  `wattage_w` int(11) DEFAULT NULL,
  `inverter_type` enum('HYBRID','OFF_GRID','ON_GRID') DEFAULT NULL,
  `capacity_kw` decimal(6,2) DEFAULT NULL,
  `capacity_kwh` decimal(6,2) DEFAULT NULL,
  `cycle_life` int(11) DEFAULT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `cost_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `stock_qty` int(11) NOT NULL DEFAULT '0',
  `warranty_months` int(11) NOT NULL,
  `installer_warranty_fee` decimal(10,2) DEFAULT '0.00',
  `supplier_id` int(11) DEFAULT NULL,
  `date_received` date NOT NULL,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `item_code` (`item_code`),
  KEY `supplier_id` (`supplier_id`),
  CONSTRAINT `inventory_hardware_ibfk_1` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`supplier_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_hardware`
--

LOCK TABLES `inventory_hardware` WRITE;
/*!40000 ALTER TABLE `inventory_hardware` DISABLE KEYS */;
INSERT INTO `inventory_hardware` VALUES (1,'PNL-JK550','PANEL','Jinko Solar','Tiger Neo 550W',550,NULL,NULL,NULL,NULL,46000.00,29900.00,76,144,500.00,1,'2026-01-10'),(2,'PNL-JK600','PANEL','Jinko Solar','Tiger Neo 600W',600,NULL,NULL,NULL,NULL,52000.00,33800.00,0,144,550.00,1,'2026-02-15'),(3,'PNL-LG450','PANEL','LONGi','Hi-MO 5 450W',450,NULL,NULL,NULL,NULL,38000.00,24700.00,60,144,450.00,2,'2025-08-20'),(4,'PNL-LG550','PANEL','LONGi','Hi-MO 6 550W',550,NULL,NULL,NULL,NULL,47500.00,30875.00,100,144,500.00,2,'2026-03-05'),(5,'PNL-CS545','PANEL','Canadian Solar','HiKu6 545W',545,NULL,NULL,NULL,NULL,46500.00,30225.00,40,120,500.00,1,'2025-06-12'),(6,'PNL-TR400','PANEL','Trina Solar','Vertex S 400W',400,NULL,NULL,NULL,NULL,34000.00,22100.00,30,120,400.00,3,'2025-03-01'),(7,'PNL-RS330','PANEL','Risen','RSM 330W',330,NULL,NULL,NULL,NULL,26000.00,16900.00,25,96,300.00,3,'2024-12-10'),(8,'INV-HW5','INVERTER','Huawei','SUN2000-5KTL Hybrid',NULL,'HYBRID',5.00,NULL,NULL,320000.00,208000.00,14,120,5000.00,1,'2026-01-20'),(9,'INV-HW8','INVERTER','Huawei','SUN2000-8KTL Hybrid',NULL,'HYBRID',8.00,NULL,NULL,450000.00,292500.00,8,120,6500.00,1,'2026-02-10'),(10,'INV-GW10','INVERTER','Growatt','SPH 10000 Hybrid',NULL,'HYBRID',10.00,NULL,NULL,520000.00,338000.00,8,60,7000.00,2,'2026-03-15'),(11,'INV-SG5','INVERTER','Sungrow','SH5.0RS Hybrid',NULL,'HYBRID',5.00,NULL,NULL,340000.00,221000.00,12,60,5000.00,2,'2026-01-25'),(12,'INV-VT3','INVERTER','Victron','MultiPlus-II 3kW',NULL,'OFF_GRID',3.00,NULL,NULL,280000.00,182000.00,6,60,4000.00,4,'2025-07-01'),(13,'INV-SM3','INVERTER','SMA','Sunny Boy 3.0',NULL,'ON_GRID',3.00,NULL,NULL,210000.00,136500.00,9,60,3500.00,3,'2025-05-18'),(14,'INV-GW3','INVERTER','Growatt','MIN 3000',NULL,'ON_GRID',3.00,NULL,NULL,165000.00,107250.00,14,60,3000.00,2,'2025-09-10'),(15,'BAT-HW5','BATTERY','Huawei','LUNA2000 5 kWh',NULL,NULL,NULL,5.00,6000,410000.00,266500.00,9,120,6000.00,1,'2026-01-20'),(16,'BAT-BY10','BATTERY','BYD','Battery-Box Premium 10.24',NULL,NULL,NULL,10.24,6000,780000.00,507000.00,6,120,9000.00,4,'2026-02-25'),(17,'BAT-PY5','BATTERY','Pylontech','US5000 4.8 kWh',NULL,NULL,NULL,4.80,6000,360000.00,234000.00,10,60,5000.00,4,'2025-10-05'),(18,'BAT-GW5','BATTERY','Growatt','ARK 5.1 kWh',NULL,NULL,NULL,5.12,6000,395000.00,256750.00,8,96,5000.00,2,'2025-08-14'),(19,'BAT-DY10','BATTERY','Dyness','Powerbox 9.6 kWh',NULL,NULL,NULL,9.60,6000,640000.00,416000.00,5,84,8000.00,3,'2025-02-20'),(20,'KIT-RF01','KIT','Generic','Roof Mounting Rail Kit (6 panels)',NULL,NULL,NULL,NULL,NULL,18000.00,11700.00,52,24,1500.00,4,'2026-01-05'),(21,'KIT-DC01','KIT','Generic','DC Cable & Connector Set 50m',NULL,NULL,NULL,NULL,NULL,12000.00,7800.00,57,24,1000.00,4,'2025-06-01'),(22,'KIT-AC01','KIT','Generic','AC Protection Box',NULL,NULL,NULL,NULL,NULL,15000.00,9750.00,32,24,1200.00,4,'2025-12-01'),(23,'KIT-ER01','KIT','Generic','Earthing & Surge Protection Kit',NULL,NULL,NULL,NULL,NULL,9000.00,5850.00,30,12,800.00,4,'2025-04-10');
/*!40000 ALTER TABLE `inventory_hardware` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sale_details`
--

DROP TABLE IF EXISTS `sale_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sale_details` (
  `detail_id` int(11) NOT NULL AUTO_INCREMENT,
  `order_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `qty` int(11) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `line_total` decimal(12,2) NOT NULL,
  `warranty_end` date DEFAULT NULL,
  PRIMARY KEY (`detail_id`),
  KEY `order_id` (`order_id`),
  KEY `item_id` (`item_id`),
  CONSTRAINT `sale_details_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `sales_orders` (`order_id`),
  CONSTRAINT `sale_details_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `inventory_hardware` (`item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sale_details`
--

LOCK TABLES `sale_details` WRITE;
/*!40000 ALTER TABLE `sale_details` DISABLE KEYS */;
INSERT INTO `sale_details` VALUES (1,1,5,8,46500.00,372000.00,'2035-10-05'),(2,1,14,1,165000.00,165000.00,'2030-10-05'),(3,1,23,1,9000.00,9000.00,'2026-10-05'),(4,2,5,12,46500.00,558000.00,'2035-10-20'),(5,2,14,1,165000.00,165000.00,'2030-10-20'),(6,2,21,1,12000.00,12000.00,'2027-10-20'),(7,2,23,1,9000.00,9000.00,'2026-10-20'),(8,3,3,10,38000.00,380000.00,'2037-11-15'),(9,3,14,1,165000.00,165000.00,'2030-11-15'),(10,3,23,1,9000.00,9000.00,'2026-11-15'),(11,4,1,10,46000.00,460000.00,'2038-04-08'),(12,4,8,1,320000.00,320000.00,'2036-04-08'),(13,4,15,1,410000.00,410000.00,'2036-04-08'),(14,4,20,2,18000.00,36000.00,'2028-04-08'),(15,4,21,1,12000.00,12000.00,'2028-04-08'),(16,4,22,1,15000.00,15000.00,'2028-04-08'),(17,5,4,20,47500.00,950000.00,'2038-04-22'),(18,5,9,1,450000.00,450000.00,'2036-04-22'),(19,5,16,1,780000.00,780000.00,'2036-04-22'),(20,5,20,4,18000.00,72000.00,'2028-04-22'),(21,5,21,1,12000.00,12000.00,'2028-04-22'),(22,5,22,1,15000.00,15000.00,'2028-04-22'),(23,6,2,8,52000.00,416000.00,'2038-05-14'),(24,6,11,1,340000.00,340000.00,'2031-05-14'),(25,6,20,2,18000.00,36000.00,'2028-05-14'),(26,6,21,1,12000.00,12000.00,'2028-05-14'),(27,7,1,12,46000.00,552000.00,'2038-06-03'),(28,7,8,1,320000.00,320000.00,'2036-06-03'),(29,7,15,2,410000.00,820000.00,'2036-06-03'),(30,7,20,2,18000.00,36000.00,'2028-06-03'),(31,7,22,1,15000.00,15000.00,'2028-06-03'),(32,8,4,24,47500.00,1140000.00,'2038-06-25'),(33,8,10,1,520000.00,520000.00,'2031-06-25'),(34,8,16,1,780000.00,780000.00,'2036-06-25'),(35,8,20,4,18000.00,72000.00,'2028-06-25'),(36,8,21,2,12000.00,24000.00,'2028-06-25'),(37,8,22,1,15000.00,15000.00,'2028-06-25'),(38,9,1,10,46000.00,460000.00,'2038-07-09'),(39,9,8,1,320000.00,320000.00,'2036-07-09'),(40,9,15,1,410000.00,410000.00,'2036-07-09'),(41,9,20,2,18000.00,36000.00,'2028-07-09'),(42,9,21,1,12000.00,12000.00,'2028-07-09'),(43,9,22,1,15000.00,15000.00,'2028-07-09'),(44,10,3,8,38000.00,304000.00,'2038-07-28'),(45,10,14,1,165000.00,165000.00,'2031-07-28'),(46,10,20,1,18000.00,18000.00,'2028-07-28'),(47,10,21,1,12000.00,12000.00,'2028-07-28'),(48,11,2,30,52000.00,1560000.00,'2038-08-12'),(49,11,9,2,450000.00,900000.00,'2036-08-12'),(50,11,16,2,780000.00,1560000.00,'2036-08-12'),(51,11,20,5,18000.00,90000.00,'2028-08-12'),(52,11,21,3,12000.00,36000.00,'2028-08-12'),(53,11,22,2,15000.00,30000.00,'2028-08-12'),(54,12,4,12,47500.00,570000.00,'2038-08-25'),(55,12,11,1,340000.00,340000.00,'2031-08-25'),(56,12,17,2,360000.00,720000.00,'2031-08-25'),(57,12,20,2,18000.00,36000.00,'2028-08-25'),(58,12,21,1,12000.00,12000.00,'2028-08-25'),(59,12,22,1,15000.00,15000.00,'2028-08-25'),(60,13,1,14,46000.00,644000.00,'2038-09-04'),(61,13,10,1,520000.00,520000.00,'2031-09-04'),(62,13,15,2,410000.00,820000.00,'2036-09-04'),(63,13,20,3,18000.00,54000.00,'2028-09-04'),(64,13,21,1,12000.00,12000.00,'2028-09-04'),(65,13,22,1,15000.00,15000.00,'2028-09-04'),(66,14,4,6,47500.00,285000.00,'2038-09-16'),(67,14,8,1,320000.00,320000.00,'2036-09-16'),(68,14,18,1,395000.00,395000.00,'2034-09-16'),(69,14,20,1,18000.00,18000.00,'2028-09-16'),(70,14,21,1,12000.00,12000.00,'2028-09-16'),(71,15,2,16,52000.00,832000.00,'2038-09-23'),(72,15,9,1,450000.00,450000.00,'2036-09-23'),(73,15,15,1,410000.00,410000.00,'2036-09-23'),(74,15,20,3,18000.00,54000.00,'2028-09-23'),(75,15,22,1,15000.00,15000.00,'2028-09-23'),(129,17,1,8,46000.00,368000.00,'2038-09-29'),(130,17,8,1,320000.00,320000.00,'2036-09-29'),(131,17,15,1,410000.00,410000.00,'2036-09-29'),(132,17,20,2,18000.00,36000.00,'2028-09-29'),(133,17,21,1,12000.00,12000.00,'2028-09-29'),(134,17,22,1,15000.00,15000.00,'2028-09-29'),(135,18,1,18,46000.00,828000.00,'2038-10-03'),(136,18,9,1,450000.00,450000.00,'2036-10-03'),(137,18,15,2,410000.00,820000.00,'2036-10-03'),(138,18,20,3,18000.00,54000.00,'2028-10-03'),(139,18,21,1,12000.00,12000.00,'2028-10-03'),(140,18,22,1,15000.00,15000.00,'2028-10-03'),(141,19,1,18,46000.00,828000.00,'2038-10-03'),(142,19,9,1,450000.00,450000.00,'2036-10-03'),(143,19,15,2,410000.00,820000.00,'2036-10-03'),(144,19,20,3,18000.00,54000.00,'2028-10-03'),(145,19,21,1,12000.00,12000.00,'2028-10-03'),(146,19,22,1,15000.00,15000.00,'2028-10-03');
/*!40000 ALTER TABLE `sale_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sales_orders`
--

DROP TABLE IF EXISTS `sales_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_orders` (
  `order_id` int(11) NOT NULL AUTO_INCREMENT,
  `customer_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `order_date` datetime DEFAULT CURRENT_TIMESTAMP,
  `subtotal` decimal(12,2) DEFAULT NULL,
  `discount` decimal(12,2) DEFAULT NULL,
  `tax` decimal(12,2) DEFAULT NULL,
  `warranty_fees` decimal(12,2) DEFAULT NULL,
  `grand_total` decimal(12,2) DEFAULT NULL,
  `down_payment` decimal(12,2) DEFAULT NULL,
  `balance_due` decimal(12,2) DEFAULT NULL,
  `total_kw` decimal(8,2) DEFAULT NULL,
  PRIMARY KEY (`order_id`),
  KEY `customer_id` (`customer_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `sales_orders_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`),
  CONSTRAINT `sales_orders_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sales_orders`
--

LOCK TABLES `sales_orders` WRITE;
/*!40000 ALTER TABLE `sales_orders` DISABLE KEYS */;
INSERT INTO `sales_orders` VALUES (1,1,2,'2025-10-05 10:15:00',546000.00,16380.00,96735.60,7800.00,634155.60,190246.68,443908.92,4.36),(2,5,2,'2025-10-20 14:30:00',744000.00,22320.00,131846.40,10800.00,864326.40,664326.40,200000.00,6.54),(3,7,3,'2025-11-15 11:00:00',554000.00,16620.00,98222.40,8300.00,643902.40,193170.72,450731.68,4.50),(4,2,2,'2026-04-08 09:45:00',1253000.00,62650.00,218079.00,21200.00,1429629.00,428888.70,1000740.30,5.50),(5,4,2,'2026-04-22 13:20:00',2279000.00,113950.00,395775.00,33700.00,2594525.00,778357.50,1816167.50,11.00),(6,6,3,'2026-05-14 10:05:00',804000.00,24120.00,142790.40,13400.00,936070.40,280821.12,655249.28,4.80),(7,8,2,'2026-06-03 15:40:00',1743000.00,87150.00,302949.00,27200.00,1985999.00,595799.70,1390199.30,6.60),(8,9,3,'2026-06-25 12:10:00',2551000.00,127550.00,442917.00,37200.00,2903567.00,871070.10,2032496.90,13.20),(9,1,2,'2026-07-09 10:30:00',1253000.00,62650.00,218079.00,21200.00,1429629.00,1429629.00,0.00,5.50),(10,3,3,'2026-07-28 16:00:00',499000.00,14970.00,88763.40,9100.00,581893.40,174568.02,407325.38,3.60),(11,10,2,'2026-08-12 11:25:00',4176000.00,208800.00,724968.00,60400.00,4752568.00,1425770.40,3326797.60,18.00),(12,5,3,'2026-08-25 14:50:00',1693000.00,84650.00,294219.00,26200.00,1928769.00,578630.70,1350138.30,6.60),(13,7,2,'2026-09-04 09:30:00',2065000.00,103250.00,359001.00,32700.00,2353451.00,706035.30,1647415.70,7.70),(14,2,3,'2026-09-16 13:45:00',1030000.00,51500.00,178920.00,15500.00,1172920.00,351876.00,821044.00,3.30),(15,6,2,'2026-09-23 10:55:00',1761000.00,88050.00,305991.00,27000.00,2005941.00,601782.30,1404158.70,9.60),(17,1,2,'2026-09-29 17:17:48',1161000.00,58050.00,202167.00,20200.00,1325317.00,397595.10,927721.90,4.40),(18,7,1,'2026-10-03 12:24:58',2179000.00,108950.00,378765.00,34200.00,2483015.00,744904.50,1738110.50,9.90),(19,8,1,'2026-10-03 12:25:24',2179000.00,108950.00,378765.00,34200.00,2483015.00,744904.50,1738110.50,9.90);
/*!40000 ALTER TABLE `sales_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `suppliers`
--

DROP TABLE IF EXISTS `suppliers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `suppliers` (
  `supplier_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `contact` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`supplier_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `suppliers`
--

LOCK TABLES `suppliers` WRITE;
/*!40000 ALTER TABLE `suppliers` DISABLE KEYS */;
INSERT INTO `suppliers` VALUES (1,'Sunrise Energy Imports','0112223344'),(2,'GreenPower Distributors','0112334455'),(3,'Asia Solar Trading','0112445566'),(4,'Lanka Electro Supplies','0112556677');
/*!40000 ALTER TABLE `suppliers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `role` enum('ADMIN','CASHIER') NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','ADMIN',1),(2,'cashier1','c246650737293ddc18fc357393db78d1ecc9d1fd1af95469115e4a29f983359a','CASHIER',1),(3,'cashier2','91b4d142823f7d20c5f08df69122de43f35f057a988d9619f6d3138485c9a203','CASHIER',0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-07  9:05:08
