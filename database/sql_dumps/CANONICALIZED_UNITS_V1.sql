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
-- Table structure for table `CANONICALIZED_UNITS_V1`
--

DROP TABLE IF EXISTS `CANONICALIZED_UNITS_V1`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CANONICALIZED_UNITS_V1` (
  `UNIT_SYMBOL` varchar(20) NOT NULL,
  `UNIT_NAME` varchar(50) NOT NULL,
  `FACTOR_COEFFICIENT` double NOT NULL,
  `FACTOR_POWER10` decimal(3,0) DEFAULT NULL,
  `METER_POWER` decimal(3,0) DEFAULT NULL,
  `GRAM_POWER` decimal(3,0) DEFAULT NULL,
  `SECOND_POWER` decimal(3,0) DEFAULT NULL,
  `AMPERE_POWER` decimal(3,0) DEFAULT NULL,
  `KELVIN_POWER` decimal(3,0) DEFAULT NULL,
  `MOLE_POWER` decimal(3,0) DEFAULT NULL,
  `CANDELA_POWER` decimal(3,0) DEFAULT NULL,
  `WHAT_MEASURES` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`UNIT_SYMBOL`,`UNIT_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CANONICALIZED_UNITS_V1`
--

LOCK TABLES `CANONICALIZED_UNITS_V1` WRITE;
/*!40000 ALTER TABLE `CANONICALIZED_UNITS_V1` DISABLE KEYS */;
INSERT INTO `CANONICALIZED_UNITS_V1` VALUES ('&#181;','micron',1,-6,1,0,0,0,0,0,0,'length'),('&#197;','&#229;ngstrom',1,-10,1,0,0,0,0,0,0,'length'),('&#937;','ohm',1,3,2,1,-3,-2,0,0,0,'electrical resistance'),('&#956;','micron',1,-6,1,0,0,0,0,0,0,'length'),('&Aring;','&aring;ngstrom',1,-10,1,0,0,0,0,0,0,'length'),('&micro;','micron',1,-6,1,0,0,0,0,0,0,'length'),('&mu;','micron',1,-6,1,0,0,0,0,0,0,'length'),('&Omega;','ohm',1,3,2,1,-3,-2,0,0,0,'electrical resistance'),('amu','atomic mass unit',1.660538,-24,0,1,0,0,0,0,0,'mass'),('atm','atmosphere',1.01325,8,-1,1,-2,0,0,0,0,'pressure'),('Ba','barye',1,2,-1,1,-2,0,0,0,0,'pressure'),('bar','bar',1,8,-1,1,-2,0,0,0,0,'pressure'),('C','coulomb',1,0,0,0,1,1,0,0,0,'electrical charge'),('cal','calorie',4.184,3,2,1,-2,0,0,0,0,'energy'),('cc','cubic centimeter',1,-6,3,0,0,0,0,0,0,'volume'),('d','day',86.4,3,0,0,1,0,0,0,0,'time'),('Da','dalton',1.660538,-24,0,1,0,0,0,0,0,'mass'),('dyn','dyne',1,-2,1,1,-2,0,0,0,0,'force'),('erg','erg',1,-4,2,1,-2,0,0,0,0,'energy'),('eV','electron-volt',1.60217653,-16,2,1,-2,0,0,0,0,'energy'),('F','farad',1,-3,-2,-1,4,2,0,0,0,'electrical capacitance'),('ft','foot',0.3048,0,1,0,0,0,0,0,0,'length'),('G','gauss',1,-1,0,1,-2,-1,0,0,0,'magnetic flux density'),('H','henry',1,3,2,1,-2,-2,0,0,0,'electrical inductance'),('h','hour',3.6,3,0,0,1,0,0,0,0,'time'),('Hz','hertz',1,0,0,0,-1,0,0,0,0,'frequency'),('in','inch',2.54,-2,1,0,0,0,0,0,0,'length'),('J','joule',1,3,2,1,-2,0,0,0,0,'energy'),('L','liter',1,-3,3,0,0,0,0,0,0,'volume'),('lm','lumen',1,0,0,0,0,0,0,0,1,'luminous flux'),('lx','lux',1,0,-2,0,0,0,0,0,1,'illuminance'),('M','molar',1,3,-3,0,0,0,0,1,0,'concentration'),('mi','mile',1.609344,3,1,0,0,0,0,0,0,'length'),('min','minute',60,0,0,0,1,0,0,0,0,'time'),('N','newton',1,3,1,1,-2,0,0,0,0,'force'),('P','poise',1,2,-1,1,-1,0,0,0,0,'viscosity'),('Pa','pascal',1,3,-1,1,-2,0,0,0,0,'pressure'),('psi','pounds per square inch',6.8948,6,-1,1,-2,0,0,0,0,'pressure'),('S','siemens',1,-3,-2,-1,3,2,0,0,0,'electrical conductance'),('St','stokes',1,-4,2,0,-1,0,0,0,0,'viscosity'),('T','tesla',1,3,0,1,-2,-1,0,0,0,'magnetic flux density'),('torr','torr',133.3224,3,-1,1,-2,0,0,0,0,'pressure'),('u','atomic mass unit',1.660538,-24,0,1,0,0,0,0,0,'mass'),('V','volt',1,3,2,1,-3,-1,0,0,0,'electrical voltage'),('W','watt',1,3,2,1,-3,0,0,0,0,'power'),('Wb','weber',1,3,2,1,-2,-1,0,0,0,'magnetic flux'),('yd','yard',0.9144,0,1,0,0,0,0,0,0,'length'),('yr','year',31.5576,6,0,0,1,0,0,0,0,'time');
/*!40000 ALTER TABLE `CANONICALIZED_UNITS_V1` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-12-30 12:31:44
