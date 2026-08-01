# Home Assistant - Full OSS setup samples

Tôi đã thêm các file mẫu để triển khai một hệ thống Home Assistant "full" bao gồm cấu hình cơ bản, Zigbee2MQTT, Z-Wave JS add-on config, ESPHome device, Node-RED flow, automations và Lovelace dashboard.

Các file đã thêm vào branch 'hass-full-setup':
- configuration.yaml
- automations.yaml
- zigbee2mqtt/configuration.yaml
- zwave_js/addon_config.yaml
- esphome/livingroom_temp.yaml
- nodered/nodered_flow.json
- lovelace/lovelace-dashboard.yaml

Hướng dẫn ngắn (bắt buộc trước khi dùng):
1) Tạo snapshot / backup full của Home Assistant trước khi thay đổi.
2) Chỉnh sửa các giá trị placeholder trong các file:
   - mqtt username/password in configuration.yaml and zigbee2mqtt/configuration.yaml
   - serial device paths (e.g., /dev/ttyUSB0, /dev/ttyACM1)
   - Wi-Fi SSID & password trong esphome/livingroom_temp.yaml
   - entity_id trong automations và Lovelace dashboard cho phù hợp với hệ của bạn
3) Import Node-RED flow: Node-RED → Import → dán nội dung file nodered/nodered_flow.json
4) Zigbee2MQTT: bật permit_join khi pairing, sau đó tắt.
5) Z-Wave JS: cấu hình device path và pair devices qua UI.
6) Lovelace: UI → Configure UI → Raw configuration → dán lovelace/lovelace-dashboard.yaml

Lưu ý bảo mật:
- Không dùng mật khẩu mặc định. Đổi mqtt_user/mqtt_password.
- Sử dụng DuckDNS + Let's Encrypt hoặc WireGuard để truy cập từ xa an toàn.

Nếu bạn muốn, tôi có thể:
- Thay thế placeholder (mqtt credentials, device paths) theo thông tin thực tế của bạn.
- Mở Pull Request từ branch 'hass-full-setup' vào default branch của repo.
- Mô tả chi tiết từng bước triển khai trong README.md (bằng tiếng Việt) hoặc thêm scripts để deploy.

Xác nhận: Tôi sẽ đợi bạn cho biết có muốn tôi tiếp tục (A) mở PR tự động, (B) thay placeholder theo thông tin của bạn, hoặc (C) dừng ở đây và bạn sẽ kiểm tra trước.