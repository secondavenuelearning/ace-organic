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
-- Table structure for table `CHAPTERS_V1`
--

DROP TABLE IF EXISTS `CHAPTERS_V1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CHAPTERS_V1` (
  `ID` decimal(38,0) NOT NULL,
  `NAME` varchar(200) NOT NULL,
  `BOOK` varchar(200) DEFAULT NULL,
  `REMARKS` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CHAPTERS_V1`
--

LOCK TABLES `CHAPTERS_V1` WRITE;
/*!40000 ALTER TABLE `CHAPTERS_V1` DISABLE KEYS */;
INSERT INTO `CHAPTERS_V1` VALUES (1,'Substitution and elimination reactions','',''),(2,'MS, IR, elemental analysis','',''),(3,'Alkenes','','An additional set of alkene problems can be found in the Stereochemistry chapter.  '),(5,'Alkanes: Structure and nomenclature','',''),(6,'Alkynes','',''),(7,'Dienes','',''),(8,'Alkanes: Reactions','','Chapter 9 in Bruice 4th ed.'),(9,'Benzene and its derivatives','',''),(10,'Carboxylic acids and derivatives','',''),(11,'Carbonyl compounds','',''),(12,'Carbonyl reactions at the &alpha;-carbon','',''),(13,'More about oxidation-reduction','',''),(14,'Stereochemistry','',''),(15,'Tutorials','',''),(16,'Carbohydrates','',''),(17,'Amino acids, peptides, and proteins','',''),(18,'Catalysis','',''),(19,'Organic mechanisms of coenzymes','',''),(20,'Lipids','',''),(21,'Nucleosides, nucleotides, nucleic acids','','A few more questions can be found in the Carbohydrates topic.'),(22,'Synthetic polymers','',''),(23,'Heterocycles and amines','','Eventually, the aliphatic amine questions in this topic should be moved to the Amines and Phosphines topic.  '),(24,'Pericyclic reactions','',''),(25,'More about multistep synthesis','',''),(26,'NMR spectroscopy','',''),(27,'Resonance, hybridization, Lewis structures, orbitals','',''),(28,'Functional groups','',''),(30,'Acidity and basicity; reaction conditions','',''),(31,'Alcohols and thiols','',''),(32,'Amines and phosphines','',''),(33,'Ethers, epoxides, sulfides','','More syntheses and reactions of ethers, epoxides, and sulfides can be found in the topic, Substitution and Elimination Reactions.  '),(34,'Organometallic compounds','','Raghu'),(36,'Development questions','','For development only.'),(37,'Questions from AWRORM','',''),(38,'Physical properties','',''),(50,'CHE 232 Exams','',''),(70,'Mechanisms introduction','',''),(100,'Vector-drawing questions','',''),(120,'CHE 230 Exams','',''),(121,'CHE 538 Exams','',''),(140,'CHE 532 exams','',''),(160,'CHE 236 Exams','','\'\'');
/*!40000 ALTER TABLE `CHAPTERS_V1` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-12-30 12:31:58
