export function File(file){
  this.file = file;
  this.fileName = file.name;
  this.fileSize = file.size;
  this.deleted = false;
}