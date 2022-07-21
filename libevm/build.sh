if [ -z "$1" ]; then
  echo "missing argument for output path"
  exit 1
fi

if [ -z "$2" ]; then
  echo "missing argument for output filename"
  exit 1
fi

go fix ./...
go generate ./...
go fmt ./...
go test ./...

mkdir -p "$1"
go build -buildmode=c-shared -o "$1/$2"
