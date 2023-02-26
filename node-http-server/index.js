const express = require('express')
const fs = require('fs');
const path = require('path');
const app = express()
const port = 3000

const ROOT_DIR = "/";

const fsExplore = (req,res) => {
    console.log("HIT > !")
    // Get path
    const dirPath  = req.path;
    dirPath.replace(".","");

    // Add root path
    const fullDirPath =  path.join(ROOT_DIR, dirPath);

    // return content of file if not dir
    if(!fs.statSync(fullDirPath).isDirectory())
        return res.send(fs.readFileSync(fullDirPath, 'utf8'));

    // Generate details for each file / dir in current dir
    const resArr = [];
    fs.readdirSync(fullDirPath).forEach(
        file =>
        {
            if(file[0] != ".")
                resArr.push({
                            "name" : file,
                            "dir" : fs.statSync(path.join(fullDirPath,file)).isDirectory(),
                            "ref" : path.join(fullDirPath,file)
                    })
        }
    );

    // Send array
    res.json(resArr);
}

app.get("*",fsExplore);
app.listen(port, () => {
    console.log(`Dir app listening on port ${port}`)
})