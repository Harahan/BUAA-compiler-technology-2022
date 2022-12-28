# BUAA compiler technology 2022
此仓库包括以下分支（每个分支均包含对应的文档以及代码）：
* ``LexicalAnalysis``: 词法分析
* ``SyntaxAnalysis``: 语法分析
* ``ErrorHandling``: 错误处理
* ``MiddleCodeGeneration``: 中间代码生成
* ``MipsCodeGeneration``: ``mips``代码生成
* ``Optimization``: 代码优化，用于竞速版本，在``fix bug(pass all)``之后提交不保证正确性
* ``FinalExam``: 完善，删去代码优化部分针对竞速的特殊窥孔，用于上机版本，最后一次提交中包括上机的所有题目的可通过代码（一份代码可以过），
上机有两个竞速点，最后才发现评测机上连变量名都不改，于是直接对输入文本替换，但只够时间换第二点。。。

最终竞速排名为：2， 7， 4， 2， 29， 9

总排：6

注：
* 非常后悔从``Optimization``的``fix bug(pass all)``提交开始就少用了十个寄存器（``Backend/Util/RegAlloc``中的``Regs``数组中注释掉了十个寄存器）还一直没有发现（为了``deBug``，``de``忘了改回来了，在``FinalExam``分支中改回），于是竞速的第五个点寄了，该点实际排名应该在20名出头，同时第三个点应该也会快一点，具体优化内容见文档
* 期中考试题目：16进制数以及``repeat-until``的语法分析
* 期末考题目：增加``bitand``标识符判断及代码生成、``ident = getint()``文法、几个简答题、一个竞速题
