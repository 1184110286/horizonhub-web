import { ElMessageBox } from "element-plus";

// 确认提示框组件化
const Confirm = (message, okfun) => {
  ElMessageBox.confirm(message, "提示", {
    "confirm-button-text": "确定",
    "cancel-button-class": "取消",
    type: "info",
  })
    .then(async () => {
      okfun();
    })
    .catch(() => {});
};

export default Confirm;
