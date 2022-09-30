
import {execSync} from 'child_process';
const script_file = __dirname + '/gen_block.sh'
export const generate_blocks = (n:number)  => {
    console.log("generating",n,"block(s)");
    const res = execSync('bash ' + script_file + ' ' + n,{encoding: 'base64'});
    console.log(res)
}