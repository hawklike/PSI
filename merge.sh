nameFile="steuejan.txt"
exists=0

if [ -a "$nameFile" ]; then
  exists=1
fi



rm "$nameFile" 2>/dev/null

echo "@author Jan Steuer" >> "$nameFile"
echo >> "$nameFile"

for file in $(ls *.java)
do
  echo "-----------------------$file-----------------------">> "$nameFile"
  cat "$file" >> "$nameFile"
  echo >> "$nameFile"
done

if [ $exists -eq 1 ]; then
  echo "File has been modified."
else
  echo "File has been created."
fi
