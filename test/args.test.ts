import {expect, test} from '@jest/globals';
import {ArgumentParser} from '../src/codetl/args';

const parser = new ArgumentParser();

/**
 * We only test that valid options are because the behavior of yargs is hard to mock when invalid options are used.
 */
test('Valid CLI options are pulled', () => {
    const parsedArgs = parser.parse(["--repository=/tmp/foo","--verbose=true", "--output=/tmp/foo.codetl"]);
    expect(parsedArgs.verbose).toBe(true);
    expect(parsedArgs.repository).toEqual("/tmp/foo");
    expect(parsedArgs.output).toEqual("/tmp/foo.codetl");
});
