=======================
|FEATURE|\ の最適化設定
=======================

この文書では、\ |FEATURE|\ のバッチアプリケーション実行時の最適化設定について説明します。

設定方法
========

|FEATURE|\ のバッチアプリケーション実行時の設定は、 `設定ファイル`_ を使う方法と `環境変数`_ を使う方法があります。

設定ファイル
------------

|FEATURE|\ に関するバッチアプリケーション実行時のパラメータは、 :file:`$ASAKUSA_HOME/m3bp/conf/m3bp.properties` に記述します。
このファイルは、\ |FEATURE| Gradle Pluginを有効にしてデプロイメントアーカイブを作成した場合にのみ含まれています。

このファイルに設定した内容は\ |FEATURE|\ のバッチアプリケーションの設定として使用され、バッチアプリケーション実行時の動作に影響を与えます。

設定ファイルはJavaのプロパティファイルのフォーマットと同様です。以下は ``m3bp.properties`` の設定例です。

**m3bp.properties**

..  code-block:: properties

    ## the max number of worker threads
    com.asakusafw.m3bp.thread.max=10

    ## the default number of partitions
    com.asakusafw.m3bp.partitions=10

環境変数
--------

|FEATURE|\ に関するバッチアプリケーション実行時のパラメータは、環境変数 ``ASAKUSA_M3BP_ARGS`` に設定することもできます。

環境変数 ``ASAKUSA_M3BP_ARGS`` の値には ``--engine-conf <key>=<value>`` という形式でパラメータを設定します。

以下は環境変数の設定例です。

..  code-block:: sh
    
    ASAKUSA_M3BP_ARGS='--engine-conf com.asakusafw.m3bp.thread.max=10'

設定ファイルと環境変数で同じプロパティが設定されていた場合、環境変数の値が利用されます。

..  hint::
    環境変数による設定は、バッチアプリケーションごとに設定を変更したい場合に便利です。
    
..  attention::
    :program:`yaess-batch.sh` などのYAESSコマンドを実行する環境と、\ |FEATURE|\ を実行する環境が異なる場合（例えばYAESSのSSH機能を利用している場合）に、
    YAESSコマンドを実行する環境の環境変数が\ |FEATURE|\ を実行する環境に受け渡されないことがある点に注意してください。
    
    YAESSではYAESSコマンドを実行する環境の環境変数をYAESSのジョブ実行先に受け渡すための機能がいくつか用意されているので、それらの機能を利用することを推奨します。
    詳しくは :asakusafw:`YAESSユーザガイド <yaess/user-guide.html>` などを参照してください。

設定項目
========

|FEATURE|\ のバッチアプリケーション実行時の設定項目は以下の通りです。

``com.asakusafw.m3bp.thread.max``
  タスクを実行するワーカースレッドの最大数を設定します。

  未設定の場合、利用可能な全てのCPUコアに対して一つずつワーカースレッドを割り当てます。

  既定値: (論理コア数)

``com.asakusafw.m3bp.thread.affinity``
  各ワーカースレッドへのCPUコアの割り当て方法を設定します。

  * ``none``

    * 特別な設定を行わず、OSによるCPUコアの割り当てを利用します

  * ``compact``

    * ワーカースレッドをCPUコアに割り当てる際に、同一ソケット上のコアから順番に割り当てていきます

  * ``scatter``

    * ワーカースレッドをCPUコアに割り当てる際に、異なるソケットのコアを順番に割り当てていきます

  既定値: ``none``

  ..  attention::
      この設定を有効(``none``\ 以外)にした場合、\ |FEATURE|\ はハードウェアの情報を参照します。
      仮想環境などでCPUコアの情報を正しく取得できない場合にはあまり効果がありません。

      また、環境によっては\ ``none``\ 以外を指定した際にエラーとなる場合があります。

``com.asakusafw.m3bp.partitions``
  scatter-gather操作(シャッフル操作)のパーティション数を設定します。

  既定値: (論理コア数の8倍)

``com.asakusafw.m3bp.output.buffer.size``
  個々の出力バッファのサイズをバイト数で設定します。

  既定値: ``4194304`` (``4MB``)

``com.asakusafw.m3bp.output.buffer.records``
  個々の出力バッファの最大レコード数を設定します。

  既定値: ``524288``


..  hidden
    ``com.asakusafw.m3bp.buffer.access``

       個々の入出力バッファのアクセス方式を設定します。

       * ``nio``

         * JavaのNIOを利用してバッファにアクセスします。

       * ``unsafe``

         * Javaの非推奨の方法を利用してバッファにアクセスします。
         * エキスパート以外はこの設定を利用するべきではありません。

       既定値: ``nio``

``hadoop.<name>``
  指定の ``<name>`` を名前に持つHadoopの設定を追加します。

  |FEATURE|\ では、一部の機能 (Direct I/Oなど) にHadoopのライブラリ群を利用しています。
  このライブラリ群がHadoopの設定を参照している場合、この項目を利用して設定値を変更できます。

  Asakusa全体に関するHadoopの設定は ``$ASAKUSA_HOME/core/conf/asakusa-resources.xml`` 内で行えますが、
  同一の項目に対する設定が ``asakusa-resources.xml`` と ``hadoop.<name>`` の両方に存在する場合、後者の設定値を優先します。
