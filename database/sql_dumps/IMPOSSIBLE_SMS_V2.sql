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
-- Table structure for table `IMPOSSIBLE_SMS_V2`
--

DROP TABLE IF EXISTS `IMPOSSIBLE_SMS_V2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `IMPOSSIBLE_SMS_V2` (
  `NAME` varchar(80) NOT NULL,
  `DEFINITION` longtext DEFAULT NULL,
  `SORTKEY` varchar(100) NOT NULL,
  PRIMARY KEY (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `IMPOSSIBLE_SMS_V2`
--

LOCK TABLES `IMPOSSIBLE_SMS_V2` WRITE;
/*!40000 ALTER TABLE `IMPOSSIBLE_SMS_V2` DISABLE KEYS */;
INSERT INTO `IMPOSSIBLE_SMS_V2` VALUES ('1-haloalcohol or primary or secondary 1-haloamine','[H][#8,#7][#6;X4]-[F,Cl,Br,I]','haloalcohol or primary or secondary 1-haloamine-1'),('acyl anion','[CH-]=O','acyl anion'),('carbonic acid derivative','[H]OC([!#1!#6])=O','carbonic acid derivative'),('compound with a leaving group #b to a carbanion or metal','[#6-]-,=[#6;!$([#6]([#7,#8])=O)]-,:[#7,#8,F,#16,Cl,Br,I]','compound with a leaving group #b to a carbanion or metal'),('cyclopentadienone without aryl substituents','[#1,C]C1=C([#1,C])C(=O)C([#1,C])=C1[#1,C] |c:1,7|','cyclopentadienone without aryl substituents'),('enol','C=CO[H]','enol'),('enolizable 2,4-cyclohexadienone','[H][C;X4]1C=CC=CC1=O |c:2,4|','enolizable 2,4-cyclohexadienone'),('enolizable 2,5-cyclohexadienone','[H][C;X4]1C=CC(=O)C=C1 |c:2,6|','enolizable 2,5-cyclohexadienone'),('formyl halide','[H]C([F,Cl,Br,I])=O','formyl halide');
/*!40000 ALTER TABLE `IMPOSSIBLE_SMS_V2` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-12-30 12:31:42
