import { checkSelected } from "../common/select";
import { getSelectedFDR } from "../common/select";
import { uploadFile } from "./upload";
import { filterFDR } from "../common/filter";
import { sendUpdatedFiles } from "./update";
import { getFDRList } from "../common/fdr";
import { getFDRText } from "../common/fdr";
import { UpdateRequest } from "./update";
import { setFileListTitle } from "../common/select";
import { showAlert } from "../common/alert";
import "./fileadd";
const chosenFiles = require("./fileadd").chosenFiles;

const fileArrayElement = document.querySelector(".chosen_files");
const fileSearchInput = document.querySelector(".search_input");

const eventSource = new EventSource(`${location.origin}/events/upload`);
eventSource.onopen = function () {
  console.log("EventSource is Open");
};
eventSource.onerror = function (e) {
  console.error(e);
};
// eventSource.addEventListener("ping", () => {});

eventSource.addEventListener("upload_file", function (e) {
  console.log("UploadRequest Received");
  const decodedRequest = decodeURI(e.data);
  const uploadRequest = JSON.parse(decodedRequest);
  const requestedFileName = uploadRequest.filename;
  const requestId = uploadRequest.request_id;
  console.log(uploadRequest);
  uploadFile(
    requestedFileName, // requested filename
    chosenFiles, // the file array
    requestId // The request id
  );
});

eventSource.addEventListener("close_connection", function () {
  eventSource.close();
  console.log("Received close-connection event >> closed the connection");
});

eventSource.addEventListener("ping", () => {
  // console.log("PING");
});

eventSource.addEventListener("reload", () => {});

const deleteSelected = document.querySelector(".delete_selected");
deleteSelected.addEventListener("click", function () {
  const updateRequestArray = [];
  const selectedFDRList = getSelectedFDR();
  if (selectedFDRList.length == 0) {
    showAlert("No files selected");
    return;
  }
  for (const selectedFDR of selectedFDRList) {
    selectedFDR.style.display = "none";
    selectedFDR.classList.remove("selected");
    selectedFDR.classList.add("deleted");
    fileArrayElement.removeChild(selectedFDR);

    const fileName = selectedFDR
      .querySelector("div.fdr_file_name span")
      .innerText.trim();
    const fileSize = selectedFDR
      .querySelector("div.fdr_file_size span")
      .innerText.trim();
    updateRequestArray.push(new UpdateRequest(fileName, fileSize, "remove"));
  }
  checkSelected();
  resetSearch();
  removeDeletedFiles();
  sendUpdatedFiles(updateRequestArray);
});

function resetSearch() {
  setFileListTitle();
  fileSearchInput.value = "";
  filterFDR("", getFDRList());
}

/* Remove the files from the chosen files Array */
function removeDeletedFiles() {
  let found = false;
  for (const fl of chosenFiles) {
    if (found) {
      /* Incase there are many files with the same filename only remove the first one in the list */
      break;
    }
    const file_name = fl.fileName;
    for (const fdr of getFDRList()) {
      if (
        fdr.classList.contains("deleted") &&
        getFDRText(fdr) == file_name.trim()
      ) {
        fl.deleted = true;
        found = true;
      }
    }
  }
}

fileSearchInput.addEventListener("input", function () {
  filterFDR(this.value, getFDRList());
});

function getUpdateFileList() {
  const fdrArray = [];
  for (const fdr of getFDRList()) {
    if (fdr.classList.contains("deleted")) {
      continue;
    }
    fdrArray.push(fdr);
  }
  return fdrArray;
}
