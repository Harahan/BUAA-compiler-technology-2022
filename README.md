# BUAA compiler technology 2022
此仓库包括以下分支（每个分支均包含对应的文档以及代码）：
* ``LexicalAnalysis``: 词法分析
* ``SyntaxAnalysis``: 语法分析
* ``ErrorHandling``: 错误处理
* ``MiddleCodeGeneration``: 中间代码生成
* ``MipsCodeGeneration``: ``mips``代码生成
* ``Optimization``: 代码优化，用于竞速版本，在``fix bug(pass all)``之后提交不保证正确性
* ``FinalExam``: 完善，删去代码优化部分针对竞速的特殊窥孔

最终竞速排名为：2， 7， 4， 2， 29， 9

总排：6

注：非常后悔从``Optimization``的``fix bug(pass all)``提交开始就少用了十个寄存器（``Backend/Util/RegAlloc``中的``Regs``数组中注释掉了十几个寄存器）还一直没有发现（为了``deBug``，``de``忘了改回来了），
于是竞速的第五个点寄了，该点实际排名应该在20名出头，同时第三个点应该也会快一点，具体优化内容见文档
