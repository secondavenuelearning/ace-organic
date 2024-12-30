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
-- Table structure for table `MENU_ONLY_REAGENTS_V1`
--

DROP TABLE IF EXISTS `MENU_ONLY_REAGENTS_V1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MENU_ONLY_REAGENTS_V1` (
  `DEFINITION` varchar(80) NOT NULL,
  `NAME` varchar(80) NOT NULL,
  PRIMARY KEY (`DEFINITION`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `MENU_ONLY_REAGENTS_V1`
--

LOCK TABLES `MENU_ONLY_REAGENTS_V1` WRITE;
/*!40000 ALTER TABLE `MENU_ONLY_REAGENTS_V1` DISABLE KEYS */;
INSERT INTO `MENU_ONLY_REAGENTS_V1` VALUES ('Br','HBr'),('BrBr','Br2'),('BrN1C(=O)CCC1=O','NBS'),('BrP(Br)Br','PBr3'),('CC1=CC=C(C=C1)S(Cl)(=O)=O','TsCl'),('Cc1ccc(cc1)S(Cl)(=O)=O','TsCl'),('Cl','HCl'),('ClCl','Cl2'),('ClN1C(=O)CCC1=O','NCS'),('ClP(Cl)(Cl)(Cl)Cl','PCl5'),('ClS(=O)Cl','SOCl2'),('Cl[Al](Cl)Cl','AlCl3'),('CS(Cl)(=O)=O','MsCl'),('II','I2'),('O=[Os](=O)(=O)=O','OsO4'),('OCCO','1,2-ethanediol'),('[H]B([H])[H]','BH3'),('[H][Al-]([H])([H])[H]','LiAlH4'),('[H][B-]([H])([H])[H]','NaBH4'),('[H][H]','H2'),('[Li]','Li'),('[Mg]','Mg'),('[NaH]','NaH');
/*!40000 ALTER TABLE `MENU_ONLY_REAGENTS_V1` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-12-30 12:31:48
