====================================
|FEATURE|\ リファレンス
====================================

この文書では、\ |FEATURE|\ が提供するGradle PluginやDSLコンパイラの設定、およびバッチアプリケーション実行時の設定などについて説明します。

|FEATURE| Gradle Plugin リファレンス
====================================

|FEATURE| Gradle Pluginが提供する機能とインターフェースについて個々に解説します。

プラグイン
----------

``asakusafw-m3bp``
    アプリケーションプロジェクトで、|FEATURE|\ のさまざまな機能を有効にする。

    このプラグインは ``asakusafw`` プラグインや ``asakusafw-organizer`` プラグインを拡張するように作られているため、それぞれのプラグインも併せて有効にする必要がある（ ``apply plugin: 'asakusafw-m3bp'`` だけではほとんどの機能を利用できません）。

タスク
------

``m3bpCompileBatchapps``
    |COMPILER|\ を利用してDSLをコンパイルする [#]_ 。

    ``asakusafw`` プラグインが有効である場合にのみ利用可能。

``attachComponentM3bp``
    デプロイメントアーカイブに\ |FEATURE|\ 向けのバッチアプリケーションを実行するためのコンポーネントを追加する。

    ``asakusafw-organizer`` プラグインが有効である場合にのみ利用可能。

    ``asakusafwOrganizer.m3bp.enabled`` に ``true`` が指定されている場合、自動的に有効になる。

``attachM3bpBatchapps``
    デプロイメントアーカイブに ``m3bpCompileBatchapps`` でコンパイルした結果を含める。

    ``asakusafw`` , ``asakusafw-organizer`` の両プラグインがいずれも有効である場合にのみ利用可能。

    ``asakusafwOrganizer.batchapps.enabled`` に ``true`` が指定されている場合、自動的に有効になる。

..  [#] :asakusa-gradle-groovydoc:`com.asakusafw.gradle.tasks.AsakusaCompileTask`

タスク拡張
----------

``assemble``
    デプロイメントアーカイブを生成する。

    ``asakuafw-m3bp`` と ``asakusafw-organizer`` プラグインがいずれも有効である場合、 ``m3bpCompileBatchapps`` が依存関係に追加される。

``attachAssemble_<profile名>``
    対象のプロファイルのデプロイメントアーカイブに必要なコンポーネントを追加する。

    ``asakuafw-m3bp`` と ``asakusafw-organizer`` プラグインがいずれも有効である場合、 ``attachComponentM3bp`` や ``attachM3bpBatchapps`` のうち有効であるものが依存関係に追加される。

    このタスクはデプロイメントアーカイブを生成する ``assembleAsakusafw`` の依存先になっているため、このタスクの依存先に上記のタスクを追加することで、デプロイメントアーカイブに必要なコンポーネントを追加できるようになっている。

規約プロパティ拡張
------------------

Batch Application Plugin ( ``asakusafw`` ) への拡張
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|FEATURE| Gradle PluginはBatch Application Pluginに対して\ |FEATURE|\ のビルド設定を行うための規約プロパティを追加します。この規約プロパティは、 ``asakusafw`` ブロック内の参照名 ``m3bp`` でアクセスできます [#]_ 。

以下、 ``build.gradle`` の設定例です。

**build.gradle**

..  code-block:: groovy

    asakusafw {
        m3bp {
            include 'com.example.batch.*'
            compilerProperties += ['m3bp.native.path': '/usr/bin:/usr/local/bin']
        }
    }

この規約オブジェクトは以下のプロパティを持ちます。

``m3bp.outputDirectory``
    コンパイラの出力先を指定する。

    文字列や ``java.io.File`` などで指定し、相対パスが指定された場合にはプロジェクトからの相対パスとして取り扱う。

    既定値: ``"$buildDir/m3bp-batchapps"``

``m3bp.include``
    コンパイルの対象に含めるバッチクラス名のパターンを指定する。

    バッチクラス名には ``*`` でワイルドカードを含めることが可能。

    また、バッチクラス名のリストを指定した場合、それらのパターンのいずれかにマッチしたバッチクラスのみをコンパイルの対象に含める。

    既定値: ``null`` (すべて)

``m3bp.exclude``
    コンパイルの対象から除外するバッチクラス名のパターンを指定する。

    バッチクラス名には ``*`` でワイルドカードを含めることが可能。

    また、バッチクラス名のリストを指定した場合、それらのパターンのいずれかにマッチしたバッチクラスをコンパイルの対象から除外する。

    ``include`` と ``exclude`` がいずれも指定された場合、 ``exclude`` のパターンを優先して取り扱う。

    既定値: ``null`` (除外しない)

``m3bp.runtimeWorkingDirectory``
    実行時のテンポラリワーキングディレクトリのパスを指定する。

    パスにはURIやカレントワーキングディレクトリからの相対パスを指定可能。

    未指定の場合、コンパイラの標準設定である「 ``target/hadoopwork`` 」を利用する。

    既定値: ``null`` (コンパイラの標準設定を利用する)

``m3bp.compilerProperties``
    `コンパイラプロパティ`_ （コンパイラのオプション設定）を追加する。

    この値はマップ型 ( ``java.util.Map`` ) であるため、プロパティのキーと値をマップのキーと値として追加可能。

    既定値: (|FEATURE|\ 向けのコンパイルに必要な最低限のもの)

``m3bp.batchIdPrefix``
    |FEATURE|\ 向けのバッチアプリケーションに付与するバッチIDの接頭辞を指定する。

    文字列を設定すると、それぞれのバッチアプリケーションは「 ``<接頭辞><本来のバッチID>`` 」というバッチIDに強制的に変更される。

    空文字や ``null`` を指定した場合、本来のバッチIDをそのまま利用するが、他のコンパイラが生成したバッチアプリケーションと同じバッチIDのバッチアプリケーションを生成した場合、アプリケーションが正しく動作しなくなる。

    既定値: ``"m3bp."``

``m3bp.failOnError``
    |FEATURE|\ 向けのコンパイルを行う際に、コンパイルエラーが発生したら即座にコンパイルを停止するかどうかを選択する。

    コンパイルエラーが発生した際に、 ``true`` を指定した場合にはコンパイルをすぐに停止し、 ``false`` を指定した場合には最後までコンパイルを実施する。

    既定値: ``true`` (即座にコンパイルを停止する)

..  [#] これらのプロパティは規約オブジェクト :asakusa-gradle-groovydoc:`com.asakusafw.gradle.plugins.AsakusafwCompilerExtension` が提供します。

Framework Organizer Plugin ( ``asakusafwOrganizer`` ) への拡張
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

|FEATURE| Gradle Plugin は Framework Organizer Plugin に対して\ |FEATURE|\ のビルド設定を行うための規約プロパティを追加します。この規約プロパティは、 ``asakusafwOrganizer`` ブロック内の参照名 ``m3bp`` でアクセスできます [#]_ 。

この規約オブジェクトは以下のプロパティを持ちます。

``m3bp.enabled``
    デプロイメントアーカイブに\ |FEATURE|\ のコンポーネント群を追加するかどうかを指定する。

    ``true`` を指定した場合にはコンポーネントを追加し、 ``false`` を指定した場合には追加しない。

    既定値: ``true`` (コンポーネント群を追加する)

``m3bp.useSystemNativeDependencies``
    デプロイメントアーカイブの\ |FEATURE|\ が、実行環境にインストールされたネイティブの依存ライブラリ群を利用するかどうかを指定する。

    ``true`` を指定した場合にはインストールされたネイティブの依存ライブラリ群を利用し、 ``false`` を指定した場合にはデプロイメントアーカイブにライブラリ群を含めてそちらを利用する。

    既定値: ``false`` (実行環境にインストールされたネイティブの依存ライブラリ群を利用しない)

    ..  note::

        この設定に ``false`` を指定することで、\ |FEATURE|\ が利用する ``boost`` などのライブラリをデプロイメントアーカイブに含めることができます。
        非標準の実行環境の構成を利用している場合や、独自に入手したライブラリを利用したい場合などにはこの設定に ``true`` を指定してください。
        また、それぞれの依存ライブラリのバージョンについては、 ``$ASAKUSA_HOME/m3bp/native`` 以下のライブラリに ``ldd`` コマンドなどを利用して確認してください。

        なお、この設定に ``false`` を指定しても全ての依存ライブラリが含まれるわけではありません。
        詳しくは :doc:`user-guide` を参照してください。

``m3bp.useSystemHadoop``
    デプロイメントアーカイブの\ |FEATURE|\ が、実行環境にインストールされているHadoopを利用するかどうかを指定する。

    ``true`` を指定した場合には環境にインストールされているHadoopを利用し、 ``false`` を指定した場合にはデプロイメントアーカイブに最小構成のHadoopライブラリ群を含めてそちらを利用する。

    既定値: ``false`` (実行環境にインストールされたHadoopを利用しない)

``<profile>.m3bp.enabled``
    対象のプロファイルに対し、デプロイメントアーカイブに\ |FEATURE|\ のコンポーネントを追加するかどうかを指定する。

    前述の ``m3bp.enabled`` と同様だが、こちらはプロファイルごとに指定できる。

    既定値: ``asakusafwOrganizer.m3bp.enabled`` (全体のデフォルト値を利用する)

``<profile>.m3bp.useSystemNativeDependencies``
    対象のプロファイルに対し、デプロイメントアーカイブの\ |FEATURE|\ が、実行環境にインストールされたネイティブの依存ライブラリ群を利用するかどうかを指定する。

    前述の ``m3bp.useSystemNativeDependencies`` と同様だが、こちらはプロファイルごとに指定できる。

    既定値: ``asakusafwOrganizer.m3bp.useSystemNativeDependencies`` (全体のデフォルト値を利用する)

``<profile>.m3bp.useSystemHadoop``
    対象のプロファイルに対し、デプロイメントアーカイブの\ |FEATURE|\ が、実行環境にインストールされているHadoopを利用するかどうかを指定する。

    前述の ``m3bp.useSystemHadoop`` と同様だが、こちらはプロファイルごとに指定できる。

    既定値: ``asakusafwOrganizer.m3bp.useSystemHadoop`` (全体のデフォルト値を利用する)

..  [#] これらのプロパティは規約オブジェクト :asakusa-m3bp-gradle-groovydoc:`com.asakusafw.m3bp.gradle.plugins.AsakusafwOrganizerM3bpExtension` が提供します。

コマンドラインオプション
------------------------

:program:`m3bpCompileBatchapps` タスクを指定して :program:`gradlew` コマンドを実行する際に、 ``m3bpCompileBatchapps --update <バッチクラス名>`` と指定することで、指定したバッチクラス名のみをバッチコンパイルすることができます。

また、バッチクラス名の文字列には ``*`` をワイルドカードとして使用することもできます。

以下の例では、パッケージ名に ``com.example.target.batch`` を含むバッチクラスのみをバッチコンパイルしてデプロイメントアーカイブを作成しています。

..  code-block:: sh

    ./gradlew m3bpCompileBatchapps --update com.example.target.batch.* assemble

そのほか、 :program:`m3bpCompileBatchapps` タスクは :program:`gradlew` コマンド実行時に以下のコマンドライン引数を指定することができます。

..  program:: m3bpCompileBatchapps

..  option:: --compiler-properties <k1=v1[,k2=v2[,...]]>

    追加のコンパイラプロパティを指定する。

    規約プロパティ ``asakusafw.m3bp.compilerProperties`` で設定したものと同じキーを指定した場合、それらを上書きする。

..  option:: --batch-id-prefix <prefix.>

    生成するバッチアプリケーションに、指定のバッチID接頭辞を付与する。

    規約プロパティ ``asakusafw.m3bp.batchIdPrefix`` の設定を上書きする。

..  option:: --fail-on-error <"true"|"false">

    コンパイルエラー発生時に即座にコンパイル処理を停止するかどうか。

    規約プロパティ ``asakusafw.m3bp.failOnError`` の設定を上書きする。

..  option:: --update <batch-class-name-pattern>

    指定のバッチクラスだけをコンパイルする (指定したもの以外はそのまま残る)。

    規約プロパティ ``asakusafw.m3bp.{in,ex}clude`` と同様にワイルドカードを利用可能。

    このオプションが設定された場合、規約プロパティ ``asakusafw.m3bp.{in,ex}clude`` の設定は無視する。

|COMPILER|\ リファレンス
========================

コンパイラプロパティ
--------------------

|COMPILER|\ で利用可能なコンパイラプロパティについて説明します。
これらの設定方法については、 `Batch Application Plugin ( asakusafw ) への拡張`_ の ``m3bp.compilerProperties`` の項を参照してください。

``inspection.dsl``
    DSLの構造を可視化するためのファイル( ``etc/inspection/dsl.json`` )を生成するかどうか。

    ``true`` ならば生成し、 ``false`` ならば生成しない。

    既定値: ``true``

``inspection.task``
    タスクの構造を可視化するためのファイル( ``etc/inspection/task.json`` )を生成するかどうか。

    ``true`` ならば生成し、 ``false`` ならば生成しない。

    既定値: ``true``

``directio.input.filter.enabled``
    Direct I/O input filterを有効にするかどうか。

    ``true`` ならば有効にし、 ``false`` ならば無効にする。

    既定値: ``true``

``operator.checkpoint.remove``
    DSLで指定した ``@Checkpoint`` 演算子をすべて除去するかどうか。

    ``true`` ならば除去し、 ``false`` ならば除去しない。

    既定値: ``true``

``operator.logging.level``
    DSLで指定した ``@Logging`` 演算子のうち、どのレベル以上を表示するか。

    ``debug`` , ``info`` , ``warn`` , ``error`` のいずれかを指定する。

    既定値: ``info``

``operator.aggregation.default``
    DSLで指定した ``@Summarize`` , ``@Fold`` 演算子の ``partialAggregate`` に ``PartialAggregation.DEFAULT`` が指定された場合に、どのように集約を行うか。

    ``total`` であれば部分集約を許さず、 ``partial`` であれば部分集約を行う。

    既定値: ``total``

``input.estimator.tiny``
    インポーター記述の ``getDataSize()`` に ``DataSize.TINY`` が指定された際、それを何バイトのデータとして見積もるか。

    値にはバイト数か、 ``+Inf`` (無限大)、 ``NaN`` (不明) のいずれかを指定する。

    主に、 ``@MasterJoin`` 系の演算子でJOINのアルゴリズムを決める際など、データサイズによる最適化の情報として利用される。

    既定値: ``10485760`` (10MB)

``input.estimator.small``
    インポーター記述の ``getDataSize()`` に ``DataSize.SMALL`` が指定された際、それを何バイトのデータとして見積もるか。

    その他については ``input.estimator.tiny`` と同様。

    既定値: ``209715200`` (200MB)

``input.estimator.large``
    インポーター記述の ``getDataSize()`` に ``DataSize.LARGE`` が指定された際、それを何バイトのデータとして見積もるか。

    その他については ``input.estimator.tiny`` と同様。

    既定値: ``+Inf`` (無限大)

``operator.join.broadcast.limit``
    ``@MasterJoin`` 系の演算子で、broadcast joinアルゴリズムを利用して結合を行うための、マスタ側の最大入力データサイズ。

    基本的には ``input.estimator.tiny`` で指定した値の2倍程度にしておくのがよい。

    既定値: ``20971520`` (20MB)

``operator.estimator.<演算子注釈名>``
    指定した演算子の入力に対する出力データサイズの割合。

    「演算子注釈名」には演算子注釈の単純名 ( ``Extract`` , ``Fold`` など) を指定し、値には割合 ( ``1.0`` , ``2.5`` など) を指定する。

    たとえば、「 ``operator.estimator.CoGroup`` 」に ``5.0`` を指定した場合、すべての ``@CoGroup`` 演算子の出力データサイズは、入力データサイズの合計の5倍として見積もられる。

    既定値: `operator.estimator.* のデフォルト値`_ を参照

``<バッチID>.<オプション名>``
    指定のオプションを、指定のIDのバッチに対してのみ有効にする。

    バッチIDは ``m3bp.`` などのプレフィックスが付与する **まえの** ものを指定する必要がある。

    既定値: N/A

``dag.planning.option.unifySubplanIo``
    等価なステージの入出力を一つにまとめる最適化を有効にするかどうか。

    ``true`` ならば有効にし、 ``false`` ならば無効にする。

    無効化した場合、ステージの入出力データが増大する場合があるため、特別な理由がなければ有効にするのがよい。

    既定値: ``true``

``dag.planning.option.checkpointAfterExternalInputs``
    ジョブフローの入力の直後にチェックポイント処理を行うかどうか。

    ``true`` ならばチェックポイント処理を行い、 ``false`` ならば行わない。

    既定値: ``false``

``m3bp.native.cmake``
    アプリケーションのコンパイル時に利用する ``CMake`` コマンドの名前またはフルパス。

    既定値: ``cmake``

``m3bp.native.make``
    アプリケーションのコンパイル時に利用する ``Make`` コマンドの名前またはフルパス。

    既定値: ``make``

``m3bp.native.path``
    アプリケーションのコンパイル時に利用する ``CMake`` や ``Make`` コマンドを探索するためのパス。

    複数のディレクトリを指定する場合、パスセパレータ文字 (Unixの場合は ``":"``) で区切って指定する。

    既定値: (``PATH`` 環境変数の値)

``m3bp.native.cmake.<name>``
    アプリケーションのコンパイル時に利用する ``CMake`` コマンドの追加オプション (``-D<name>``)。

    たとえば、 ``m3bp.native.cmake.CMAKE_BUILD_TYPE`` に ``Debug`` を指定することで、ビルドタイプを ``Debug`` に変更できる。

operator.estimator.* のデフォルト値
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

..  list-table:: operator.estimator.* のデフォルト値
    :widths: 3 7
    :header-rows: 1

    * - 演算子注釈名
      - 計算式
    * - ``Checkpoint``
      - 入力の ``1.0`` 倍
    * - ``Logging``
      - 入力の ``1.0`` 倍
    * - ``Branch``
      - 入力の ``1.0`` 倍
    * - ``Project``
      - 入力の ``1.0`` 倍
    * - ``Extend``
      - 入力の ``1.25`` 倍
    * - ``Restructure``
      - 入力の ``1.25`` 倍
    * - ``Split``
      - 入力の ``1.0`` 倍
    * - ``Update``
      - 入力の ``2.0`` 倍
    * - ``Convert``
      - 入力の ``2.0`` 倍
    * - ``Summarize``
      - 入力の ``1.0`` 倍
    * - ``Fold``
      - 入力の ``1.0`` 倍
    * - ``MasterJoin``
      - トランザクション入力の ``2.0`` 倍
    * - ``MasterJoinUpdate``
      - トランザクション入力の ``2.0`` 倍
    * - ``MasterCheck``
      - トランザクション入力の ``1.0`` 倍
    * - ``MasterBranch``
      - トランザクション入力の ``1.0`` 倍
    * - ``Extract``
      - 既定値無し
    * - ``GroupSort``
      - 既定値無し
    * - ``CoGroup``
      - 既定値無し

既定値がない演算子に対しては、有効なデータサイズの見積もりを行いません。

制限事項
========

ここでは、\ |FEATURE|\ 固有の制限事項について説明します。これらの制限は将来のバージョンで緩和される可能性があります。

非対応機能
----------

|FEATURE|\ は、Asakusa Frameworkが提供する以下の機能には対応していません。

* ThunderGate
* レガシーモジュール
* その他該当バージョンで非推奨となっている機能

互換性について
==============

ここでは\ |FEATURE|\ を利用する場合に考慮すべき、Asakusa Frameworkやバッチアプリケーションの互換性について説明します。

演算子の互換性
--------------

|FEATURE|\ では、バッチアプリケーション内の演算子内に定義したstaticフィールドを複数のスレッドから利用する場合があります。
このため、演算子クラス内でフィールドにstaticを付与している場合、staticの指定を除去するかフィールド参照がスレッドセーフになるようにしてください。

