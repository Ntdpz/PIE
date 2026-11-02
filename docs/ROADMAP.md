# Roadmap

## Foundation ที่ทำในรอบวางโครง

- [x] Gradle wrapper + version catalog + Kotlin/Compose
- [x] แยก 8 modules และตรวจ dependency boundaries
- [x] Manual constructor DI และ injectable Clock
- [x] หน้าหลัก/เขียน/ปฏิทิน/ตั้งค่าขั้นต้น
- [x] Room หนึ่งบันทึกต่อวันและ schema export
- [x] Serial autosave, revision status, failure/retry, pending draft ใน process
- [x] โลโก้และ adaptive launcher icon
- [x] Unit tests สำหรับ write pipeline
- [x] Android database persistence test source

ช่องด้านบนระบุ implementation; ผลรันทดสอบต้องดูจากคำสั่งและรายงาน build ของแต่ละเครื่อง ไม่ถือว่า instrumentation ผ่านเพียงเพราะมี source

## งานถัดไปตามลำดับ

1. **Editor reliability + UX**
   - จับ Thai IME, cursor/selection, editor reopen, กดย้อนระหว่างบันทึก
   - process restart, low storage, failed read/write, วันที่/เขตเวลา
   - ลบทั้งวันพร้อม confirmation และเอา state ของวันที่ลบออกจาก write pipeline อย่างปลอดภัย
   - accessibility, large font, animation ตาม system duration scale
2. **Media**
   - System picker → staging copy → commit attachment metadata
   - cleanup partial files, space check, cancel import, thumbnail
   - Coil / Media3 เมื่อเริ่มใช้งานจริง ยังไม่ดึง dependency ที่ไม่ได้ใช้
3. **PIN + local protection**
   - ตั้ง/ยืนยัน/เปลี่ยน/ปิด PIN, rate limiting, lock lifecycle
   - ซ่อน recent apps, ตัดสินใจ encryption และ keystore strategy
4. **Reminders**
   - DataStore, permission flow, day/time schedule, reboot/time-zone reschedule
   - ข้อความรายวัน, custom mode, test notification
5. **Backup / Restore**
   - Archive versioning, password encryption, streaming media
   - ส่งออก consistent snapshot พร้อมสื่อที่เสร็จแล้ว
   - นำเข้าใน staging ตรวจครบแล้ว commit เฉพาะเมื่อแอปว่าง
6. **Local usage + release**
   - event definition, no content collection, no upload
   - Baseline Profile, release APK size/startup/frame measurements
   - signing นอก repo, install/update test และ V1 acceptance

## V2

Google Drive Backup / ย้ายเครื่อง / Sync โดยคง local database เป็น source of truth
