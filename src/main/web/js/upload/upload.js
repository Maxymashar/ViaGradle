/* This function should be asynchronous */
export function uploadFile(fileName,fileArray,requestId) {
  const file = getFile(fileName, fileArray);
  const xhr = new XMLHttpRequest();
  xhr.open("POST", "/request_file_download");
  xhr.setRequestHeader("FileName",encodeURI(file.name));
  xhr.setRequestHeader("RequestId",requestId);
  xhr.setRequestHeader("Type","NotNull");
  xhr.send(file);
}
function getFile(file_name, file_array) {
  for (const fl of file_array) {
    if (fl.fileName.trim() == file_name.trim()) {
      console.log("Found File");
      return fl.file;
    }
  }
}
