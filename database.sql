-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: civil_services_transfer_system
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `civil_services_transfer_system`
--

/*!40000 DROP DATABASE IF EXISTS `civil_services_transfer_system`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `civil_services_transfer_system` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `civil_services_transfer_system`;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `log_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `role` varchar(50) DEFAULT NULL,
  `action` varchar(100) NOT NULL,
  `entity` varchar(50) NOT NULL,
  `entity_id` int DEFAULT NULL,
  `description` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `fk_audit_user` (`user_id`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (1,2,'CIVIL_SERVICE_OFFICER','SUBMIT_REQUEST','transfer_requests',1,'Officer Arun Kumar submitted a transfer request for Chennai.','2026-09-09 15:33:56'),(2,1,'CADRE_ADMINISTRATOR','VERIFY_DOCUMENT','documents',1,'Administrator verified medical document for Request #1.','2026-09-09 15:33:56'),(3,7,'TRANSFER_COMMITTEE_MEMBER','APPROVE_REQUEST','transfer_requests',1,'Transfer Committee Member approved Request #1.','2026-09-09 15:33:56'),(4,1,'CADRE_ADMINISTRATOR','GENERATE_ORDER','transfer_orders',1,'Transfer Order GO-TN-REV-2026-0941 generated for Officer Arun Kumar.','2026-09-09 15:33:56'),(5,2,'CIVIL_SERVICE_OFFICER','LOGIN','users',2,'User logged in: officer1','2026-09-09 16:04:19'),(6,2,'CIVIL_SERVICE_OFFICER','SUBMIT_REQUEST','transfer_requests',4,'Submitted transfer request for Chennai Secretariat','2026-09-09 16:04:59'),(7,1,'CADRE_ADMINISTRATOR','LOGIN','users',1,'User logged in: admin','2026-09-09 16:05:23'),(8,2,'CIVIL_SERVICE_OFFICER','LOGOUT','users',2,'User logged out: officer1','2026-09-09 16:05:24'),(9,1,'CADRE_ADMINISTRATOR','LOGIN','users',1,'User logged in: admin','2026-09-09 16:05:52');
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `documents` (
  `document_id` int NOT NULL AUTO_INCREMENT,
  `request_id` int NOT NULL,
  `document_name` varchar(150) NOT NULL,
  `document_type` varchar(100) NOT NULL,
  `document_url` varchar(255) DEFAULT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `verification_status` varchar(20) DEFAULT 'PENDING',
  `verified_by` int DEFAULT NULL,
  `verification_comments` text DEFAULT NULL,
  `uploaded_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`document_id`),
  KEY `fk_documents_request` (`request_id`),
  CONSTRAINT `fk_documents_request` FOREIGN KEY (`request_id`) REFERENCES `transfer_requests` (`request_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` VALUES (1,1,'Medical_Certificate_ArunKumar.pdf','Medical','uploads/docs/medical_cso1001.pdf','uploads/docs/medical_cso1001.pdf','VERIFIED',1,'Verified by Admin: Valid hospital document','2026-09-09 15:33:56'),(2,2,'Spouse_Employment_Certificate.pdf','Spouse','uploads/docs/spouse_cso1002.pdf','uploads/docs/spouse_cso1002.pdf','VERIFIED',1,'Verified by Admin: Employment certificate confirmed','2026-09-09 15:33:56'),(3,3,'Personal_Representation_Letter.pdf','Personal','uploads/docs/personal_cso1003.pdf','uploads/docs/personal_cso1003.pdf','PENDING',NULL,NULL,'2026-09-09 15:33:56');
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `notification_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `title` varchar(150) NOT NULL,
  `message` text NOT NULL,
  `notification_type` varchar(50) DEFAULT 'INFO',
  `read_status` varchar(20) DEFAULT 'UNREAD',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notification_id`),
  KEY `fk_notifications_user` (`user_id`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,2,'Transfer Request Approved','Your transfer request (REQ-1) to Chennai Secretariat has been approved by the Transfer Committee.','SUCCESS','UNREAD','2026-09-09 15:33:56'),(2,2,'Transfer Order Generated','Official Transfer Order GO-TN-REV-2026-0941 has been issued. Please review and acknowledge.','ACTION_REQUIRED','UNREAD','2026-09-09 15:33:56'),(3,3,'Request Under Committee Review','Your transfer request (REQ-2) has been forwarded to the Transfer Committee for review.','INFO','READ','2026-09-09 15:33:56'),(4,1,'New Transfer Request Submitted','New request #4 submitted by Officer.','INFO','UNREAD','2026-09-09 16:04:59');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `officers`
--

DROP TABLE IF EXISTS `officers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `officers` (
  `officer_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `employee_id` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `joining_date` date DEFAULT NULL,
  `designation` varchar(100) DEFAULT NULL,
  `department` varchar(100) DEFAULT NULL,
  `cadre` varchar(100) DEFAULT NULL,
  `current_posting` varchar(100) DEFAULT NULL,
  `current_district` varchar(100) DEFAULT NULL,
  `current_state` varchar(100) DEFAULT 'Tamil Nadu',
  `years_in_current_posting` int DEFAULT '0',
  `previous_transfer_date` date DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  PRIMARY KEY (`officer_id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `employee_id` (`employee_id`),
  CONSTRAINT `fk_officers_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `officers`
--

LOCK TABLES `officers` WRITE;
/*!40000 ALTER TABLE `officers` DISABLE KEYS */;
INSERT INTO `officers` VALUES (1,2,'CSO1001','Arun Kumar','arun.kumar@civilservices.tn.gov.in','9876543210','1985-05-12','2010-08-01','District Collector','Revenue Administration','IAS','Madurai Collectorate','Madurai','Tamil Nadu',6,'2020-07-15','ACTIVE'),(2,3,'CSO1002','Priya Sharma','priya.sharma@civilservices.tn.gov.in','9876543211','1988-11-24','2013-06-15','Joint Commissioner','Commercial Taxes','IAS','Salem Head Office','Salem','Tamil Nadu',4,'2022-01-10','ACTIVE'),(3,4,'CSO1003','Ravi Kumar','ravi.kumar@civilservices.tn.gov.in','9876543212','1990-03-30','2016-01-20','Sub-Collector','Revenue Department','IAS','Trichy Office','Trichy','Tamil Nadu',2,'2024-03-01','ACTIVE'),(4,5,'CSO1004','Meena Devi','meena.devi@civilservices.tn.gov.in','9876543213','1987-09-08','2012-09-10','Additional Collector','Rural Development','IAS','Erode Collectorate','Erode','Tamil Nadu',5,'2021-08-20','ACTIVE'),(5,6,'CSO1005','Karthik Raj','karthik.raj@civilservices.tn.gov.in','9876543214','1989-12-18','2015-04-05','Deputy Commissioner','Transport','IAS','Tirunelveli Circle','Tirunelveli','Tamil Nadu',3,'2023-05-11','ACTIVE');
/*!40000 ALTER TABLE `officers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
  `review_id` int NOT NULL AUTO_INCREMENT,
  `request_id` int NOT NULL,
  `reviewer_id` int NOT NULL,
  `recommendation` varchar(30) NOT NULL,
  `remarks` text,
  `review_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `fk_reviews_request` (`request_id`),
  KEY `fk_reviews_user` (`reviewer_id`),
  CONSTRAINT `fk_reviews_request` FOREIGN KEY (`request_id`) REFERENCES `transfer_requests` (`request_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reviews_user` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES (1,1,7,'APPROVE','Officer has served 6 years in current posting. High priority medical case with available vacancy in Chennai.','2026-08-12 10:30:00'),(2,2,7,'APPROVE','Spouse co-location valid case. Recommend approval for Coimbatore posting.','2026-08-18 05:15:00');
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transfer_orders`
--

DROP TABLE IF EXISTS `transfer_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfer_orders` (
  `order_id` int NOT NULL AUTO_INCREMENT,
  `request_id` int NOT NULL,
  `officer_id` int NOT NULL,
  `order_number` varchar(50) NOT NULL,
  `old_location` varchar(100) NOT NULL,
  `new_location` varchar(100) NOT NULL,
  `order_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `joining_date` date NOT NULL,
  `status` varchar(20) DEFAULT 'GENERATED',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `request_id` (`request_id`),
  UNIQUE KEY `order_number` (`order_number`),
  KEY `fk_orders_officer` (`officer_id`),
  CONSTRAINT `fk_orders_officer` FOREIGN KEY (`officer_id`) REFERENCES `officers` (`officer_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_orders_request` FOREIGN KEY (`request_id`) REFERENCES `transfer_requests` (`request_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transfer_orders`
--

LOCK TABLES `transfer_orders` WRITE;
/*!40000 ALTER TABLE `transfer_orders` DISABLE KEYS */;
INSERT INTO `transfer_orders` VALUES (1,1,1,'GO-TN-REV-2026-0941','Madurai Collectorate','Chennai Secretariat','2026-08-13 03:30:00','2026-09-15','ACKNOWLEDGED');
/*!40000 ALTER TABLE `transfer_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transfer_policies`
--

DROP TABLE IF EXISTS `transfer_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfer_policies` (
  `policy_id` int NOT NULL AUTO_INCREMENT,
  `policy_name` varchar(150) NOT NULL,
  `description` text,
  `minimum_service_years` int NOT NULL DEFAULT '3',
  `maximum_transfer_frequency` int NOT NULL DEFAULT '1',
  `priority_rules` varchar(255) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  PRIMARY KEY (`policy_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transfer_policies`
--

LOCK TABLES `transfer_policies` WRITE;
/*!40000 ALTER TABLE `transfer_policies` DISABLE KEYS */;
INSERT INTO `transfer_policies` VALUES (1,'General Service Tenure Policy','Officers must serve a minimum of 3 consecutive years in their current posting before requesting a routine transfer.',3,1,'Standard seniority and posting duration rules apply.','ACTIVE'),(2,'Spouse & Family Co-Location Policy','Priority consideration given to officers requesting transfer for spouse co-location in government service.',2,1,'Spouse employment proof required.','ACTIVE'),(3,'Medical & Special Care Policy','Immediate priority processing for officers or dependents requiring specialized medical treatment.',1,2,'Medical board verification mandatory.','ACTIVE');
/*!40000 ALTER TABLE `transfer_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transfer_requests`
--

DROP TABLE IF EXISTS `transfer_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfer_requests` (
  `request_id` int NOT NULL AUTO_INCREMENT,
  `officer_id` int NOT NULL,
  `request_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `reason` text NOT NULL,
  `preferred_location_1` varchar(100) NOT NULL,
  `preferred_location_2` varchar(100) DEFAULT NULL,
  `preferred_location_3` varchar(100) DEFAULT NULL,
  `status` varchar(30) DEFAULT 'SUBMITTED',
  `eligibility_status` varchar(30) DEFAULT 'PENDING_VERIFICATION',
  `optimization_score` int DEFAULT '0',
  `priority` varchar(20) DEFAULT 'LOW',
  `remarks` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`request_id`),
  KEY `fk_requests_officer` (`officer_id`),
  CONSTRAINT `fk_requests_officer` FOREIGN KEY (`officer_id`) REFERENCES `officers` (`officer_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transfer_requests`
--

LOCK TABLES `transfer_requests` WRITE;
/*!40000 ALTER TABLE `transfer_requests` DISABLE KEYS */;
INSERT INTO `transfer_requests` VALUES (1,1,'2026-08-10 04:30:00','Medical reasons and family care in capital city.','Chennai','Coimbatore','Salem','APPROVED','ELIGIBLE',93,'HIGH','Verified medical certificate. Eligible and recommended for approval.','2026-09-09 15:33:56','2026-09-09 15:33:56'),(2,2,'2026-08-15 06:00:00','Spouse working in Coimbatore District Court.','Coimbatore','Chennai','Madurai','COMMITTEE_REVIEW','ELIGIBLE',78,'HIGH','Documents verified. Forwarded to committee for final review.','2026-09-09 15:33:56','2026-09-09 15:33:56'),(3,3,'2026-08-20 08:45:00','Personal request for location change.','Chennai','Coimbatore','Thanjavur','SUBMITTED','PENDING_VERIFICATION',45,'LOW','Initial submission awaiting administrator document verification.','2026-09-09 15:33:56','2026-09-09 15:33:56'),(4,1,'2026-09-09 16:04:59','Medical grounds for spouse care in Chennai','Chennai Secretariat','Coimbatore Civil Station','','VERIFIED','ELIGIBLE',100,'HIGH','Eligible for transfer processing.','2026-09-09 16:04:59','2026-09-09 16:06:34');
/*!40000 ALTER TABLE `transfer_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `role` enum('CIVIL_SERVICE_OFFICER','CADRE_ADMINISTRATOR','TRANSFER_COMMITTEE_MEMBER') NOT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','cb7bddc90ba5cc6afbc2f5c61919682c8915633837eaac3747344db7293443e7','Cadre Administrator','admin@civilservices.tn.gov.in','CADRE_ADMINISTRATOR','ACTIVE','2026-09-09 15:33:56'),(2,'officer1','7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7','Arun Kumar','arun.kumar@civilservices.tn.gov.in','CIVIL_SERVICE_OFFICER','ACTIVE','2026-09-09 15:33:56'),(3,'officer2','7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7','Priya Sharma','priya.sharma@civilservices.tn.gov.in','CIVIL_SERVICE_OFFICER','ACTIVE','2026-09-09 15:33:56'),(4,'officer3','7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7','Ravi Kumar','ravi.kumar@civilservices.tn.gov.in','CIVIL_SERVICE_OFFICER','ACTIVE','2026-09-09 15:33:56'),(5,'officer4','7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7','Meena Devi','meena.devi@civilservices.tn.gov.in','CIVIL_SERVICE_OFFICER','ACTIVE','2026-09-09 15:33:56'),(6,'officer5','7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7','Karthik Raj','karthik.raj@civilservices.tn.gov.in','CIVIL_SERVICE_OFFICER','ACTIVE','2026-09-09 15:33:56'),(7,'committee1','203fb45e01d0f509abf65e53410bcb7736809a2be454eba03a65b8677ab23631','Dr. S. Ramanathan','committee@civilservices.tn.gov.in','TRANSFER_COMMITTEE_MEMBER','ACTIVE','2026-09-09 15:33:56');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vacancies`
--

DROP TABLE IF EXISTS `vacancies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vacancies` (
  `vacancy_id` int NOT NULL AUTO_INCREMENT,
  `location` varchar(100) NOT NULL,
  `district` varchar(100) NOT NULL,
  `state` varchar(100) DEFAULT 'Tamil Nadu',
  `department` varchar(100) NOT NULL,
  `designation` varchar(100) NOT NULL,
  `cadre` varchar(100) NOT NULL,
  `available_positions` int NOT NULL DEFAULT '1',
  `status` varchar(20) DEFAULT 'AVAILABLE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`vacancy_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vacancies`
--

LOCK TABLES `vacancies` WRITE;
/*!40000 ALTER TABLE `vacancies` DISABLE KEYS */;
INSERT INTO `vacancies` VALUES (1,'Chennai Secretariat','Chennai','Tamil Nadu','Revenue Administration','District Collector','IAS',3,'AVAILABLE','2026-09-09 15:33:56'),(2,'Coimbatore Civil Station','Coimbatore','Tamil Nadu','Commercial Taxes','Joint Commissioner','IAS',2,'AVAILABLE','2026-09-09 15:33:56'),(3,'Madurai Collectorate','Madurai','Tamil Nadu','Rural Development','Additional Collector','IAS',1,'AVAILABLE','2026-09-09 15:33:56'),(4,'Salem Head Office','Salem','Tamil Nadu','Transport','Deputy Commissioner','IAS',4,'AVAILABLE','2026-09-09 15:33:56'),(5,'Trichy District Office','Trichy','Tamil Nadu','Revenue Department','Sub-Collector','IAS',2,'AVAILABLE','2026-09-09 15:33:56');
/*!40000 ALTER TABLE `vacancies` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-23 20:45:29
