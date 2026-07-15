-- 추천 설정값 범위 컬럼 추가 및 데이터 갱신 (6만장 EXIF 통계 기반)
-- 1) 스키마 확장
ALTER TABLE expert_setting_statistics
  ADD COLUMN iso_min INT NULL,
  ADD COLUMN iso_max INT NULL,
  ADD COLUMN shutter_min VARCHAR(20) NULL,
  ADD COLUMN shutter_max VARCHAR(20) NULL;

-- 2) 데이터 기반 추천값 갱신
UPDATE expert_setting_statistics SET iso=200, iso_min=50, iso_max=1600, shutter='1/200', shutter_min='1/8', shutter_max='1/2478' WHERE scene='contrast' AND brightness='bright';
UPDATE expert_setting_statistics SET iso=270, iso_min=64, iso_max=4000, shutter='1/125', shutter_min='1/2', shutter_max='1/2000' WHERE scene='contrast' AND brightness='dark';
UPDATE expert_setting_statistics SET iso=200, iso_min=50, iso_max=2500, shutter='1/200', shutter_min='1/3', shutter_max='1/2000' WHERE scene='contrast' AND brightness='normal';
UPDATE expert_setting_statistics SET iso=200, iso_min=50, iso_max=2000, shutter='1/120', shutter_min='1/6', shutter_max='1/640' WHERE scene='food' AND brightness='bright';
UPDATE expert_setting_statistics SET iso=400, iso_min=80, iso_max=3200, shutter='1/80', shutter_min='1/5', shutter_max='1/640' WHERE scene='food' AND brightness='dark';
UPDATE expert_setting_statistics SET iso=320, iso_min=64, iso_max=3200, shutter='1/60', shutter_min='1/6', shutter_max='1/604' WHERE scene='food' AND brightness='normal';
UPDATE expert_setting_statistics SET iso=100, iso_min=50, iso_max=453, shutter='1/444', shutter_min='1/1', shutter_max='1/2000' WHERE scene='landscape' AND brightness='bright';
UPDATE expert_setting_statistics SET iso=100, iso_min=50, iso_max=800, shutter='1/200', shutter_min='3.97s', shutter_max='1/1993' WHERE scene='landscape' AND brightness='dark';
UPDATE expert_setting_statistics SET iso=100, iso_min=50, iso_max=640, shutter='1/320', shutter_min='1.3s', shutter_max='1/2000' WHERE scene='landscape' AND brightness='normal';
UPDATE expert_setting_statistics SET iso=400, iso_min=100, iso_max=2175, shutter='1/250', shutter_min='1.133s', shutter_max='1/675' WHERE scene='night' AND brightness='bright';
UPDATE expert_setting_statistics SET iso=500, iso_min=100, iso_max=6400, shutter='1/3', shutter_min='30s', shutter_max='1/315' WHERE scene='night' AND brightness='dark';
UPDATE expert_setting_statistics SET iso=400, iso_min=100, iso_max=6400, shutter='1/50', shutter_min='30s', shutter_max='1/500' WHERE scene='night' AND brightness='normal';
UPDATE expert_setting_statistics SET iso=200, iso_min=100, iso_max=1600, shutter='1/200', shutter_min='1/20', shutter_max='1/1600' WHERE scene='portrait' AND brightness='bright';
UPDATE expert_setting_statistics SET iso=320, iso_min=100, iso_max=6400, shutter='1/125', shutter_min='1/20', shutter_max='1/1250' WHERE scene='portrait' AND brightness='dark';
UPDATE expert_setting_statistics SET iso=250, iso_min=100, iso_max=4000, shutter='1/200', shutter_min='1/20', shutter_max='1/2000' WHERE scene='portrait' AND brightness='normal';
