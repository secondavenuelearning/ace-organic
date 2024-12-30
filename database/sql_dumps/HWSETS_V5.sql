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
-- Table structure for table `HWSETS_V5`
--

DROP TABLE IF EXISTS `HWSETS_V5`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `HWSETS_V5` (
  `HW_ID` decimal(38,0) NOT NULL,
  `COURSE_ID` decimal(38,0) DEFAULT NULL,
  `SERIAL_NO` decimal(38,0) DEFAULT NULL,
  `NAME` longtext DEFAULT NULL,
  `REMARKS` longtext DEFAULT NULL,
  `DATE_CREATED` varchar(50) DEFAULT NULL,
  `DATE_DUE` varchar(50) DEFAULT NULL,
  `TRIES` decimal(38,0) DEFAULT NULL,
  `FLAGS` decimal(38,0) DEFAULT NULL,
  `MAX_EXTENSION_STR` varchar(10) DEFAULT NULL,
  `DEPENDS_ON` decimal(38,0) DEFAULT NULL,
  `DURATION` decimal(38,0) DEFAULT NULL,
  PRIMARY KEY (`HW_ID`),
  KEY `F_HWS5_COURSE` (`COURSE_ID`),
  KEY `F_HWS5_DEP` (`DEPENDS_ON`),
  CONSTRAINT `F_HWS5_COURSE` FOREIGN KEY (`COURSE_ID`) REFERENCES `CW_COURSES_V3` (`ID`) ON DELETE NO ACTION,
  CONSTRAINT `F_HWS5_DEP` FOREIGN KEY (`DEPENDS_ON`) REFERENCES `HWSETS_V5` (`HW_ID`) ON DELETE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `HWSETS_V5`
--

LOCK TABLES `HWSETS_V5` WRITE;
/*!40000 ALTER TABLE `HWSETS_V5` DISABLE KEYS */;
INSERT INTO `HWSETS_V5` VALUES (3127,928,1,'LewisJS tutorial','','4/30/2008 8:16','12/31/2099 6:00',-1,3,'-1',NULL,NULL),(8209,928,16,'Organometallic mechanisms (advanced students only)','','8/26/2014 13:42','12/31/2099 6:00',-1,2,'-1',NULL,30),(8284,928,5,'Energy diagrams tutorial','','11/12/2014 13:13','12/31/2099 6:00',-1,3,'-1',NULL,30),(8286,928,3,'MarvinJS tutorial','This tutorial is for the Javascript version of Marvin, not the Java applet version.','11/19/2014 17:24','12/31/2099 6:00',-1,3,'-1',NULL,30),(8287,928,7,'MarvinJS stereochemistry tutorial','','11/20/2014 13:44','12/31/2099 6:00',-1,3,'-1',NULL,NULL),(8305,928,9,'MarvinJS mechanism tutorial','','11/25/2014 14:13','12/31/2099 6:00',-1,3,'-1',NULL,30),(8306,928,11,'MarvinJS multistep synthesis tutorial','','11/25/2014 15:57','12/31/2099 6:00',-1,3,'-1',NULL,NULL),(8375,928,15,'MarvinJS SRN1 mechanism tutorial (advanced students only)','Only students taking advanced classes will need to work this tutorial.','5/24/2015 23:02','12/31/2099 6:00',-1,3,'-1',NULL,NULL),(8658,928,13,'MarvinJS radical mechanisms tutorial','','4/20/2016 14:47','12/31/2099 6:00',-1,3,'-1',NULL,NULL),(8903,928,4,'MarvinJS resonance structures tutorial','','1/9/2017 13:21','12/31/2099 6:00',-1,3,'-1',NULL,30);
/*!40000 ALTER TABLE `HWSETS_V5` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-12-30 12:31:41
