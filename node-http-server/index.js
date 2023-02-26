// Run this using node to start directory http server
const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const port = 3000;

const ROOT_DIR = '/';

const fsExplore = async (req, res) => {
    console.log('HIT > !');
    // Get path
    let dirPath = req.path;
    dirPath = path.join(ROOT_DIR, dirPath);
    // Remove file extension
    dirPath = path.parse(dirPath).dir + '/' + path.parse(dirPath).name;

    try {
        const stat = await fs.promises.stat(dirPath);
        if (stat.isFile()) {
            const content = await fs.promises.readFile(dirPath, 'utf8');
            return res.send(content);
        } else if (stat.isDirectory()) {
            const files = await fs.promises.readdir(dirPath);
            const resArr = files
                .filter(file => !file.startsWith('.'))
                .map(file => {
                    const filePath = path.join(dirPath, file);
                    const fileStat = fs.statSync(filePath);
                    return {
                        name: file,
                        dir: fileStat.isDirectory(),
                        ref: path.join(req.path, file),
                    };
                });
            res.json(resArr);
        } else {
            throw new Error(`Invalid path: ${dirPath}`);
        }
    } catch (error) {
        console.error(error);
        res.status(500).send('Internal Server Error');
    }
};

app.get('*', fsExplore);

app.listen(port, () => {
    console.log(`Dir app listening on port ${port}`);
});
