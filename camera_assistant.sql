-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: camera_assistant
-- ------------------------------------------------------
-- Server version	8.0.44

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
-- Dumping data for table `expert_setting_statistics`
--

LOCK TABLES `expert_setting_statistics` WRITE;
/*!40000 ALTER TABLE `expert_setting_statistics` DISABLE KEYS */;
INSERT INTO `expert_setting_statistics` VALUES (1,'food','bright',100,'1/200','2026-03-10 15:36:17'),(2,'food','normal',200,'1/100','2026-03-10 15:36:17'),(3,'food','dark',800,'1/60','2026-03-10 15:36:17'),(4,'portrait','bright',100,'1/250','2026-03-10 15:36:17'),(5,'portrait','normal',200,'1/125','2026-03-10 15:36:17'),(6,'portrait','dark',1600,'1/60','2026-03-10 15:36:17'),(7,'landscape','bright',50,'1/500','2026-03-10 15:36:17'),(8,'landscape','normal',100,'1/250','2026-03-10 15:36:17'),(9,'landscape','dark',400,'1/30','2026-03-10 15:36:17'),(10,'night','bright',400,'1/60','2026-03-10 15:36:17'),(11,'night','normal',800,'1/30','2026-03-10 15:36:17'),(12,'night','dark',3200,'1/15','2026-03-10 15:36:17'),(13,'contrast','bright',100,'1/200','2026-03-10 15:36:17'),(14,'contrast','normal',200,'1/100','2026-03-10 15:36:17'),(15,'contrast','dark',800,'1/60','2026-03-10 15:36:17');
/*!40000 ALTER TABLE `expert_setting_statistics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `guide_text`
--

LOCK TABLES `guide_text` WRITE;
/*!40000 ALTER TABLE `guide_text` DISABLE KEYS */;
INSERT INTO `guide_text` VALUES (16,'food','bright','음식이 좀 밝게 나왔어요 카메라 설정에서 밝기를 살짝 낮춰보세요','직사광선보다 창가 간접광이 음식을 훨씬 맛있어 보이게 해줘요','2026-03-10 19:57:54'),(17,'food','normal','노출은 딱 좋아요 이제 구도만 조금 신경써보세요','위에서 45도 각도로 찍으면 음식이 더 풍성해 보여요','2026-03-10 19:57:54'),(18,'food','dark','음식이 좀 어둡게 나왔어요 카메라 설정에서 밝기를 올려보세요','창가 쪽으로 자리를 옮기면 자연광만으로도 훨씬 밝아져요','2026-03-10 19:57:54'),(19,'portrait','bright','인물이 좀 밝게 나왔어요 카메라 설정에서 밝기를 살짝 낮춰보세요','역광이라면 피사체 위치를 조금만 바꿔보세요','2026-03-10 19:57:54'),(20,'portrait','normal','노출은 딱 좋아요 배경을 흐리게 찍으면 인물이 더 돋보여요','렌즈를 피사체에 최대한 가까이 두면 배경이 부드럽게 날아가요','2026-03-10 19:57:54'),(21,'portrait','dark','인물이 좀 어둡게 나왔어요 카메라 설정에서 밝기를 올려보세요','눈에 빛이 살짝 들어오도록 각도를 조절해보면 생동감이 달라져요','2026-03-10 19:57:54'),(22,'landscape','bright','풍경이 좀 밝게 나왔어요 카메라 설정에서 밝기를 낮춰보세요','구름이 있는 날 찍으면 하늘이 훨씬 풍성하게 나와요','2026-03-10 19:57:54'),(23,'landscape','normal','노출은 딱 좋아요 수평선만 맞춰주면 완성이에요','전경에 돌이나 꽃 같은 요소를 넣으면 사진에 깊이가 생겨요','2026-03-10 19:57:54'),(24,'landscape','dark','풍경이 좀 어둡게 나왔어요 카메라 설정에서 밝기를 올려보세요','일출 직후나 일몰 직전에 찍으면 분위기가 완전히 달라져요','2026-03-10 19:57:54'),(25,'night','bright','야경이 좀 밝게 나왔어요 카메라 설정에서 밝기를 낮춰보세요','빛의 궤적을 살리고 싶다면 셔터를 좀 더 오래 열어두세요','2026-03-10 19:57:54'),(26,'night','normal','노출은 딱 좋아요 흔들리지 않게 찍는 게 핵심이에요','숨을 참고 셔터를 누르거나 폰을 벽에 기대보세요','2026-03-10 19:57:54'),(27,'night','dark','야경이 좀 어두워요 카메라 설정에서 밝기를 올리고 최대한 고정해서 찍어보세요','손떨림이 있으면 야경이 흔들려요 폰을 평평한 곳에 올려두고 찍어보세요','2026-03-10 19:57:54'),(28,'contrast','bright','밝고 어두운 부분 차이가 큰데 좀 밝게 나왔어요 밝기를 살짝 낮춰보세요','빛이 강한 날은 그늘진 곳에서 찍으면 훨씬 부드럽게 나와요','2026-03-10 19:57:54'),(29,'contrast','normal','명암이 잘 살아있어요 이대로 찍어도 충분해요','빛과 그림자 경계선을 구도 요소로 활용하면 더 드라마틱해져요','2026-03-10 19:57:54'),(30,'contrast','dark','밝고 어두운 부분 차이가 큰데 전체적으로 어두워요 밝기를 올려보세요','어두운 곳에서는 최대한 빛이 있는 방향으로 피사체를 향하게 해보세요','2026-03-10 19:57:54');
/*!40000 ALTER TABLE `guide_text` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `user_history_log`
--

LOCK TABLES `user_history_log` WRITE;
/*!40000 ALTER TABLE `user_history_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_history_log` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-10 20:02:05
