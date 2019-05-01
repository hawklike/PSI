nameFile="steuejan.txt"
tmpFile="tmp"

echo "@author Jan Steuer" >> "$tmpFile"
echo >> "$tmpFile"

for file in $(ls ./src/cz/fit/cvut/steuejan/psi/serverClient/*.java)
do
  name=$(echo "$file" | rev | cut -d/ -f1 | rev)
  echo "-----------------------"$name"-----------------------">> "$tmpFile"
  cat "$file" >> "$tmpFile"
  echo >> "$tmpFile"
done

if [ -a "$nameFile" ]; then
  #files are the same
  if diff "$nameFile" "$tmpFile" 2>/dev/null 1>&2; then
    echo "Everything is up to date."
    rm "$tmpFile"
    exit 0;
  else
    echo "File has been modified."
  fi
else
  echo "File has been created."
fi

mv "$tmpFile" "$nameFile"
