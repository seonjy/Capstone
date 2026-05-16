-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: camera_assistant
-- ------------------------------------------------------
-- Server version	8.0.44

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
-- Table structure for table `expert_setting_statistics`
--

DROP TABLE IF EXISTS `expert_setting_statistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expert_setting_statistics` (
  `id` int NOT NULL AUTO_INCREMENT,
  `scene` varchar(20) NOT NULL,
  `brightness` enum('bright','normal','dark') NOT NULL,
  `iso` int NOT NULL,
  `shutter` varchar(20) NOT NULL,
  `aperture` varchar(20) NOT NULL,
  `focal_length` varchar(20) NOT NULL,
  `ev` varchar(20) NOT NULL,
  `white_balance` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_scene_brightness` (`scene`,`brightness`),
  CONSTRAINT `fk_es_scene` FOREIGN KEY (`scene`) REFERENCES `scene_type` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `expert_setting_statistics`
--

LOCK TABLES `expert_setting_statistics` WRITE;
/*!40000 ALTER TABLE `expert_setting_statistics` DISABLE KEYS */;
INSERT INTO `expert_setting_statistics` VALUES (1,'food','bright',100,'1/200','f/4.0','50mm','0.0','5500K','2026-05-16 12:44:41'),(2,'food','normal',200,'1/100','f/2.8','50mm','+0.3','4000K','2026-05-16 12:44:41'),(3,'food','dark',800,'1/60','f/2.0','50mm','+0.7','3200K','2026-05-16 12:44:41'),(4,'landscape','bright',50,'1/500','f/11','24mm','-0.3','5500K','2026-05-16 12:44:41'),(5,'landscape','normal',100,'1/250','f/8.0','24mm','0.0','5500K','2026-05-16 12:44:41'),(6,'landscape','dark',400,'1/30','f/5.6','24mm','+0.3','6500K','2026-05-16 12:44:41'),(7,'night','bright',400,'1/60','f/5.6','35mm','-0.7','4000K','2026-05-16 12:44:41'),(8,'night','normal',800,'1/30','f/4.0','35mm','-0.3','3800K','2026-05-16 12:44:41'),(9,'night','dark',3200,'1/15','f/2.8','35mm','0.0','3500K','2026-05-16 12:44:41'),(10,'contrast','bright',100,'1/200','f/8.0','50mm','-1.0','5500K','2026-05-16 12:44:41'),(11,'contrast','normal',200,'1/100','f/5.6','50mm','-0.7','5200K','2026-05-16 12:44:41'),(12,'contrast','dark',800,'1/60','f/4.0','50mm','-0.3','5000K','2026-05-16 12:44:41'),(13,'portrait','bright',100,'1/250','f/2.8','85mm','0.0','5500K','2026-05-16 12:44:41'),(14,'portrait','normal',200,'1/125','f/1.8','85mm','+0.3','5000K','2026-05-16 12:44:41'),(15,'portrait','dark',1600,'1/60','f/1.4','85mm','+0.7','3500K','2026-05-16 12:44:41');
/*!40000 ALTER TABLE `expert_setting_statistics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guide_text`
--

DROP TABLE IF EXISTS `guide_text`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guide_text` (
  `id` int NOT NULL AUTO_INCREMENT,
  `scene` varchar(20) NOT NULL,
  `brightness` enum('bright','normal','dark') NOT NULL,
  `message` varchar(500) DEFAULT NULL,
  `tip` varchar(500) DEFAULT NULL,
  `guide_1_label` varchar(20) DEFAULT NULL,
  `guide_1_text` varchar(255) DEFAULT NULL,
  `guide_2_label` varchar(20) DEFAULT NULL,
  `guide_2_text` varchar(255) DEFAULT NULL,
  `guide_3_label` varchar(20) DEFAULT NULL,
  `guide_3_text` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_guide_scene_brightness` (`scene`,`brightness`),
  CONSTRAINT `fk_gt_scene` FOREIGN KEY (`scene`) REFERENCES `scene_type` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guide_text`
--

LOCK TABLES `guide_text` WRITE;
/*!40000 ALTER TABLE `guide_text` DISABLE KEYS */;
INSERT INTO `guide_text` VALUES (1,'food','bright','음식 사진이 살짝 밝게 찍혔어요. 노출을 살짝 낮추면 음식의 색감이 살아나요.','밝은 환경에서는 그림자가 진해지기 쉬워요. 자연광이 부드럽게 들어오는 자리에서 찍어보세요.','밝기','빛이 강하면 음식 위 그림자가 진해져요. 살짝 그늘진 자리에서 잡아보세요','거리','음식에 가까이 다가갈수록 뒷배경이 자연스럽게 흐려져요','각도','위에서나 약 15도 기울이면 형태가 가장 잘 드러나요','2026-05-16 12:52:22'),(2,'food','normal','음식의 색감과 질감이 잘 살아있어요.','음식 사진은 따뜻한 색감과 얕은 심도가 핵심이에요. 메인 음식이 돋보이도록 잡아주세요.','색감','따뜻한 톤으로 잡으면 음식이 더 먹음직스러워 보여요','거리','음식에 가까이 다가갈수록 뒷배경이 자연스럽게 흐려져요','각도','위에서나 약 15도 기울이면 형태가 가장 잘 드러나요','2026-05-16 12:52:22'),(3,'food','dark','음식이 너무 어두워요. ISO를 높이고 손 흔들림을 주의하세요.','어두운 환경에서는 흔들림에 약해요. 폰을 단단히 고정하고 셔터를 천천히 눌러보세요.','흔들림','폰을 양손으로 잡고 팔꿈치를 올려두면 흔들릴 일이 없어요','거리','음식에 가까이 다가갈수록 뒷배경이 자연스럽게 흐려져요','각도','위에서나 약 15도 기울이면 형태가 가장 잘 드러나요','2026-05-16 12:52:22'),(4,'landscape','bright','하늘이 다소 밝게 찍혔어요. 노출을 살짝 낮추면 구름과 색감이 더 살아나요.','강한 햇빛에서는 하늘이 환하게 날아갈 수 있어요. 살짝 어둡게 찍어 구름의 디테일을 살려보세요.','밝기','하늘이 너무 환하면 카메라를 살짝 아래로 향하면 균형이 잡혀요','구도','화면을 가로 세로 3등분해서 주제를 선 위에 두면 안정감이 생겨요','깊이','멀리 있는 것뿐 아니라 가까운 것도 함께 넣으면 입체감이 살아요','2026-05-16 12:52:22'),(5,'landscape','normal','풍경의 깊이감과 색감이 자연스럽게 표현됐어요.','풍경 사진은 깊은 심도와 수평이 핵심이에요. 전경부터 원경까지 또렷하게 잡아주세요.','수평','멀리 보이는 수평선을 기울이지 않고 뜨게 잡아주세요','구도','화면을 가로 세로 3등분해서 주제를 선 위에 두면 안정감이 생겨요','깊이','멀리 있는 것뿐 아니라 가까운 것도 함께 넣으면 입체감이 살아요','2026-05-16 12:52:22'),(6,'landscape','dark','풍경이 어두워요. ISO를 높이고 폰을 고정하면 흔들림 없이 찍을 수 있어요.','저녁이나 흐린 날에는 빛이 부족해요. 폰을 단단히 고정하고 셔터를 천천히 눌러보세요.','고정','빛이 부족할 땐 폰을 단단히 잡거나 무언가에 기대야 또렷해져요','구도','화면을 가로 세로 3등분해서 주제를 선 위에 두면 안정감이 생겨요','깊이','멀리 있는 것뿐 아니라 가까운 것도 함께 넣으면 입체감이 살아요','2026-05-16 12:52:22'),(7,'night','bright','야경의 화려한 빛이 강하게 표현됐어요. 노출을 낮추면 빛의 디테일이 살아나요.','네온이나 가로등 같은 강한 빛이 있을 땐 노출을 낮춰 빛이 번지지 않게 해주세요.','밝기','화려한 빛이 한쪽으로 치우치게 두면 더 분위기 있어요','빛모양','밝은 점이 화면의 절반을 넘지 않도록 구도를 잡으면 분위기가 살아요','고정','폰이 조금이라도 움직이면 흐릿해져요. 단단하게 고정해주세요','2026-05-16 12:52:22'),(8,'night','normal','야경의 분위기와 빛의 흐름이 잘 표현됐어요.','야경은 노이즈 억제와 광원 보존이 핵심이에요. 폰을 고정하고 천천히 찍어보세요.','셔터','셔터 누르는 순간 숨을 잠깐 멈추면 흔들림이 줄어들어요','빛모양','밝은 점이 화면의 절반을 넘지 않도록 구도를 잡으면 분위기가 살아요','고정','폰이 조금이라도 움직이면 흐릿해져요. 단단하게 고정해주세요','2026-05-16 12:52:22'),(9,'night','dark','야경이 너무 어두워요. ISO를 높이고 삼각대나 단단한 받침을 이용해 보세요.','매우 어두운 환경에서는 흔들림이 가장 큰 적이에요. 폰을 평평한 곳에 올려두고 찍어보세요.','지지대','벽이나 난간 같은 평평한 곳에 폰을 기대고 찍으면 흔들림이 거의 없어요','빛모양','밝은 점이 화면의 절반을 넘지 않도록 구도를 잡으면 분위기가 살아요','고정','폰이 조금이라도 움직이면 흐릿해져요. 단단하게 고정해주세요','2026-05-16 12:52:22'),(10,'contrast','bright','밝은 부분과 어두운 부분의 차이가 강하게 표현됐어요.','강한 빛과 그림자가 만나는 장면이에요. 밝은 부분을 기준으로 노출을 잡아주세요.','기준','가장 밝은 부분을 화면에서 톡 눌러 밝기 기준을 맞춰주세요','경계','빛과 그림자가 만나는 선을 구도에 살리면 인상적이 돼요','방향','주제를 빛이 오는 쪽으로 조금 돌리면 표정이 살아나요','2026-05-16 12:52:22'),(11,'contrast','normal','빛과 그림자의 균형이 자연스러워요.','명암 사진은 하이라이트 보존이 핵심이에요. 약간 어둡게 잡아 디테일을 살려주세요.','노출','약간 어둡게 잡으면 밝은 부분의 디테일이 살아요','경계','빛과 그림자가 만나는 선을 구도에 살리면 인상적이 돼요','방향','주제를 빛이 오는 쪽으로 조금 돌리면 표정이 살아나요','2026-05-16 12:52:22'),(12,'contrast','dark','어두운 영역이 많아요. ISO를 높이면 그림자 속 디테일도 살릴 수 있어요.','어두운 환경에서 강한 명암을 표현할 땐 흔들림에 특히 주의해주세요.','고정','어두울수록 흔들리기 쉬워요. 폰을 단단히 잡아주세요','경계','빛과 그림자가 만나는 선을 구도에 살리면 인상적이 돼요','방향','주제를 빛이 오는 쪽으로 조금 돌리면 표정이 살아나요','2026-05-16 12:52:22'),(13,'portrait','bright','인물 사진이 살짝 밝아요. 그늘진 자리로 옮기면 피부톤이 더 자연스러워져요.','강한 직사광은 얼굴에 진한 그림자를 만들어요. 그늘이나 부드러운 빛이 있는 자리를 찾아보세요.','그늘','햇빛이 강하면 그늘진 자리로 옮기면 얼굴이 부드러워져요','초점','눈동자를 한번 톡 눌러서 초점을 맞춰주세요','여백','얼굴이 보는 방향 쪽에 여백을 더 남기면 자연스러워요','2026-05-16 12:52:22'),(14,'portrait','normal','피부톤과 배경의 분리가 자연스러워요.','인물 사진은 얕은 심도와 눈 초점이 핵심이에요. 눈동자에 정확히 초점을 맞춰주세요.','어깨','눈동자는 화면 위쪽 1/3 지점에, 어깨는 잘리지 않게 잡아주세요','초점','눈동자를 한번 톡 눌러서 초점을 맞춰주세요','여백','얼굴이 보는 방향 쪽에 여백을 더 남기면 자연스러워요','2026-05-16 12:52:22'),(15,'portrait','dark','인물이 어두워요. ISO를 높이고 빛이 있는 쪽으로 살짝 돌아서 보세요.','어두운 환경에서는 빛 방향이 중요해요. 빛이 있는 쪽으로 얼굴을 살짝 돌려보세요.','빛방향','빛이 있는 쪽으로 얼굴을 살짝 돌리면 또렷하게 표현돼요','초점','눈동자를 한번 톡 눌러서 초점을 맞춰주세요','여백','얼굴이 보는 방향 쪽에 여백을 더 남기면 자연스러워요','2026-05-16 12:52:22');
/*!40000 ALTER TABLE `guide_text` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `scene_type`
--

DROP TABLE IF EXISTS `scene_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scene_type` (
  `id` varchar(20) NOT NULL,
  `display_name` varchar(20) NOT NULL,
  `english_name` varchar(30) NOT NULL,
  `accent_color` char(7) NOT NULL,
  `sort_order` tinyint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `scene_type`
--

LOCK TABLES `scene_type` WRITE;
/*!40000 ALTER TABLE `scene_type` DISABLE KEYS */;
INSERT INTO `scene_type` VALUES ('contrast','명암','Contrast','#0D253D',4,'2026-05-16 12:44:22'),('food','음식','Food','#EA2261',1,'2026-05-16 12:44:22'),('landscape','풍경','Landscape','#15BE53',2,'2026-05-16 12:44:22'),('night','야경','Night','#665EFD',3,'2026-05-16 12:44:22'),('portrait','인물','Portrait','#F96BEE',5,'2026-05-16 12:44:22');
/*!40000 ALTER TABLE `scene_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_history_log`
--

DROP TABLE IF EXISTS `user_history_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_history_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `scene` varchar(20) NOT NULL,
  `brightness` enum('bright','normal','dark') NOT NULL,
  `confidence` float DEFAULT NULL,
  `input_iso` int DEFAULT NULL,
  `input_shutter` varchar(20) DEFAULT NULL,
  `recommended_iso` int NOT NULL,
  `recommended_shutter` varchar(20) NOT NULL,
  `recommended_aperture` varchar(20) DEFAULT NULL,
  `recommended_focal_length` varchar(20) DEFAULT NULL,
  `recommended_ev` varchar(20) DEFAULT NULL,
  `recommended_wb` varchar(20) DEFAULT NULL,
  `image_path` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_created_at` (`created_at` DESC),
  KEY `idx_scene` (`scene`),
  CONSTRAINT `fk_uhl_scene` FOREIGN KEY (`scene`) REFERENCES `scene_type` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

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

-- Dump completed on 2026-05-16 13:52:32
