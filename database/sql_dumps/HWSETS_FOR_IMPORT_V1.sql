-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: ace-test.cfhbeohlo28o.us-east-1.rds.amazonaws.com    Database: C##JR
-- ------------------------------------------------------
-- Server version	5.5.5-10.11.9-MariaDB-log

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
-- Table structure for table `HWSETS_FOR_IMPORT_V1`
--

DROP TABLE IF EXISTS `HWSETS_FOR_IMPORT_V1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `HWSETS_FOR_IMPORT_V1` (
  `ID` decimal(3,0) NOT NULL,
  `NAME` longtext DEFAULT NULL,
  `CONTENTS` longtext DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `HWSETS_FOR_IMPORT_V1`
--

LOCK TABLES `HWSETS_FOR_IMPORT_V1` WRITE;
/*!40000 ALTER TABLE `HWSETS_FOR_IMPORT_V1` DISABLE KEYS */;
INSERT INTO `HWSETS_FOR_IMPORT_V1` VALUES (1,'Organic Chemistry Survey (1 semester)','<assignments>'),(21,'Art of Writing Reasonable Organic Reaction Mechanisms','<assignments>'),(22,'Organic Chemistry I (ordered like Klein)','<assignments>'),(23,'Organic Chemistry II (ordered like Klein)','<assignments>');
/*!40000 ALTER TABLE `HWSETS_FOR_IMPORT_V1` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-12-30 12:31:36
