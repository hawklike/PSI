fileName="steuejan.txt"
relativeAddress="./src/cz/fit/cvut/steuejan/psi/serverClient"
language="java"
author="Jan Steuer"

echo "@author $author" >> tmp
echo >> tmp

for file in $(ls "$relativeAddress"/*."$language")
do
  name=$(echo "$file" | rev | cut -d/ -f1 | rev)
  echo "-----------------------"$name"-----------------------" >> tmp
  cat "$file" >> tmp
  echo >> tmp
done

if [ -a "$fileName" ]; then
  #files are the same
  if diff "$fileName" tmp 2>/dev/null 1>&2; then
    echo "Everything is up to date."
    rm tmp
    exit 0;
  else
    echo "File has been modified."
  fi
else
  echo "File has been created."
fi

mv tmp "$fileName"
