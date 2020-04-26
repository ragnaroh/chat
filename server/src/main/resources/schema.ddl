
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `Room`
--

DROP TABLE IF EXISTS `Room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Room` (
  `KEY` INT(11) NOT NULL AUTO_INCREMENT,
  `ID` VARCHAR(6) NOT NULL,
  `NAME` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`KEY`),
  UNIQUE KEY `_RoomUKId` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `User`
--

DROP TABLE IF EXISTS `User`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `User` (
  `KEY` INT(11) NOT NULL AUTO_INCREMENT,
  `ID` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`KEY`),
  UNIQUE KEY `_UserUKId` (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RoomUser`
--

DROP TABLE IF EXISTS `RoomUser`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RoomUser` (
  `ROOM_KEY` INT(11) NOT NULL,
  `USER_KEY` INT(11) NOT NULL,
  `USERNAME` VARCHAR(16) NOT NULL,
  `STATUS` ENUM('PENDING','ACTIVE','INACTIVE') NOT NULL,
  PRIMARY KEY (`ROOM_KEY`,`USER_KEY`),
  KEY `_RoomUserKUserKey` (`USER_KEY`),
  CONSTRAINT `_RoomUserFKRoomKey` FOREIGN KEY (`ROOM_KEY`) REFERENCES `Room` (`KEY`),
  CONSTRAINT `_RoomUserFKUserKey` FOREIGN KEY (`USER_KEY`) REFERENCES `User` (`KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RoomEvent`
--

DROP TABLE IF EXISTS `RoomEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RoomEvent` (
  `ROOM_KEY` INT(11) NOT NULL,
  `SEQUENCE_NUMBER` INT(11) NOT NULL,
  `USER_KEY` INT(11) NOT NULL,
  `TYPE` ENUM('MESSAGE','JOINED','PARTED') NOT NULL,
  `TIMESTAMP` DATETIME NOT NULL,
  PRIMARY KEY (`ROOM_KEY`,`SEQUENCE_NUMBER`),
  KEY `_RoomEventKRoomKeyUserKey` (`ROOM_KEY`,`USER_KEY`),
  CONSTRAINT `_RoomEventFKRoomKey` FOREIGN KEY (`ROOM_KEY`) REFERENCES `Room` (`KEY`),
  CONSTRAINT `_RoomEventFKRoomKeyUserKey` FOREIGN KEY (`ROOM_KEY`,`USER_KEY`) REFERENCES `RoomUser` (`ROOM_KEY`,`USER_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `RoomMessageEvent`
--

DROP TABLE IF EXISTS `RoomMessageEvent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `RoomMessageEvent` (
  `ROOM_KEY` INT(11) NOT NULL,
  `SEQUENCE_NUMBER` INT(11) NOT NULL,
  `TEXT` text NOT NULL,
  PRIMARY KEY (`ROOM_KEY`,`SEQUENCE_NUMBER`),
  CONSTRAINT `_RoomMessageEventFKRoomKeySequenceNumber` FOREIGN KEY (`ROOM_KEY`,`SEQUENCE_NUMBER`) REFERENCES `RoomEvent` (`ROOM_KEY`,`SEQUENCE_NUMBER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
