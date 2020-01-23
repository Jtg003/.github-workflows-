import json
from collections import defaultdict
from enum import Enum

import yaml

from .writable import _filter_out_nones
from .writable import FileFormat
from .writable import Writable
from .errors import DuplicateError
from .errors import NotFoundError
from .transformation_catalog import Transformation
from .transformation_catalog import TransformationCatalog
from .replica_catalog import ReplicaCatalog
from .replica_catalog import File
from .site_catalog import SiteCatalog
from .mixins import MetadataMixin
from .mixins import HookMixin
from .mixins import ProfileMixin

PEGASUS_VERSION = "5.0"

__all__ = ["Job", "DAX", "DAG", "Workflow"]


class AbstractJob(HookMixin, ProfileMixin, MetadataMixin):
    """An abstract representation of a workflow job"""

    def __init__(self, _id=None, node_label=None):
        """
        :param _id: a unique id, if None is given then one will be assigned when this job is added to a :py:class:`~Pegasus.dax4.workflow.Workflow`, defaults to None
        :type _id: str, optional
        :param node_label: a short descriptive label that can be assined to this job, defaults to None
        :type node_label: str, optional
        """
        self._id = _id
        self.node_label = node_label
        self.args = list()
        self.uses = set()

        self.stdout = None
        self.stderr = None
        self.stdin = None

        self.hooks = defaultdict(list)
        self.profiles = defaultdict(dict)
        self.metadata = dict()

    def add_inputs(self, *input_files):
        """Add one or more :py:class:`~Pegasus.dax4.replica_catalog.File`s as input to this job
        
        :param input_files: the :py:class:`~Pegasus.dax4.replica_catalog.File`s to be added as inputs to this job
        :raises DuplicateError: all input files must be unique
        :raises ValueError: job inputs must be of type :py:class:`~Pegasus.dax4.replica_catalog.File`
        :return: self
        """
        for file in input_files:
            if not isinstance(file, File):
                raise ValueError("a job input must be of type File")

            _input = _Use(
                file, _LinkType.INPUT, register_replica=False, stage_out=False
            )
            if _input in self.uses:
                raise DuplicateError(
                    "file {0} already added as input to this job".format(file.lfn)
                )

            self.uses.add(_input)

        return self

    def get_inputs(self):
        """Get this job's input files
        
        :return: all input files associated with this job
        :rtype: set
        """
        return {use.file for use in self.uses if use._type == "input"}

    def add_outputs(self, *output_files, stage_out=True, register_replica=False):
        """Add one or more :py:class:`~Pegasus.dax4.replica_catalog.File`s as outputs to this job. stage_out and register_replica
        will be applied to all files given.
        
        :param output_files: the :py:class:`~Pegasus.dax4.replica_catalog.File` s to be added as outputs to this job
        :param stage_out: whether or not to send files back to an output directory, defaults to True
        :type stage_out: bool, optional
        :param register_replica: whether or not to register replica with a :py:class:`~Pegasus.dax4.replica_catalog.ReplicaCatalog`, defaults to False
        :type register_replica: bool, optional
        :raises DuplicateError: all output files must be unique 
        :raises ValueError: a job output must be of type File 
        :return: self
        """
        for file in output_files:
            if not isinstance(file, File):
                raise ValueError("a job output must be of type File")

            output = _Use(
                file,
                _LinkType.OUTPUT,
                stage_out=stage_out,
                register_replica=register_replica,
            )
            if output in self.uses:
                raise DuplicateError(
                    "file {0} already added as output to this job".format(file.lfn)
                )

            self.uses.add(output)

        return self

    def get_outputs(self):
        """Get this job's output files
        
        :return: all output files associated with this job 
        :rtype: set
        """
        return {use.file for use in self.uses if use._type == "output"}

    def add_checkpoint(self, checkpoint_file, stage_out=True, register_replica=False):
        """Add an output file of this job as a checkpoint file
        
        :param checkpoint_file: the :py:class:`~Pegasus.dax4.replica_catalog.File` to be added as a checkpoint file to this job
        :type checkpoint_file: File
        :param stage_out: whether or not to send files back to an output directory, defaults to True
        :type stage_out: bool, optional
        :param register_replica: whether or not to register replica with a :py:class:`~Pegasus.dax4.replica_catalog.ReplicaCatalog`, defaults to False
        :type register_replica: bool, optional
        :raises DuplicateError: all output files must be unique 
        :raises ValueError: a job output must be of type File 
        :return: self
        """

        if not isinstance(checkpoint_file, File):
            raise ValueError("checkpoint file must be of type File")

        checkpoint = _Use(
            checkpoint_file,
            _LinkType.CHECKPOINT,
            stage_out=stage_out,
            register_replica=register_replica,
        )

        if checkpoint in self.uses:
            raise DuplicateError(
                "file {0} already added as output to this job".format(
                    checkpoint_file.lfn
                )
            )

        self.uses.add(checkpoint)

        return self

    def add_args(self, *args):
        """Add arguments to this job. Each argument will be separated by a space.
        Each argument must be either a File or a primitive type. 
        
        :return: self
        :rtype: AbstractJob
        """
        self.args.extend(args)

        return self

    def set_stdin(self, file):
        """Set stdin to a :py:class:`~Pegasus.dax4.replica_catalog.File`
        
        :param file: a file that will be read into stdin  
        :type file: File or str
        :raises ValueError: file must be of type :py:class:`~Pegasus.dax4.replica_catalog.File` or str
        :raises DuplicateError: stdin is already set or the given file has already been added as an input to this job
        :return: self
        """
        if not isinstance(file, File) and not isinstance(file, str):
            raise ValueError("file must be of type File or str")

        if self.stdin is not None:
            raise DuplicateError("stdin has already been set to a file")

        if isinstance(file, str):
            file = File(file)

        self.add_inputs(file)
        self.stdin = file

        return self

    def get_stdin(self):
        """Get the :py:class:`~Pegasus.dax4.replica_catalog.File` being used for stdin
        
        :return: the stdin file
        :rtype: File
        """
        return self.stdin

    def set_stdout(self, file):
        """Set stdout to a :py:class:`~Pegasus.dax4.replica_catalog.File`
        
        :param file: a file that stdout will be written to
        :type file: File or
        :raises ValueError: file must be of type :py:class:`~Pegasus.dax4.replica_catalog.File` or str
        :raises DuplicateError: stdout is already set or the given file has already been added as an output to this job 
        :return: self
        """
        if not isinstance(file, File) and not isinstance(file, str):
            raise ValueError("file must be of type File or str")

        if self.stdout is not None:
            raise DuplicateError("stdout has already been set to a file")

        if isinstance(file, str):
            file = File(file)

        self.add_outputs(file)
        self.stdout = file

        return self

    def get_stdout(self):
        """Get the :py:class:`~Pegasus.dax4.replica_catalog.File` being used for stdout
        
        :return: the stdout file
        :rtype: File
        """
        return self.stdout

    def set_stderr(self, file):
        """Set stderr to a :py:class:`~Pegasus.dax4.replica_catalog.File` 
        
        :param file: a file that stderr will be written to
        :type file: File or str
        :raises ValueError: file must be of type File or str
        :raises DuplicateError: stderr is already set or the given file has already been added as an output to this job 
        :return: self
        """
        if not isinstance(file, File) and not isinstance(file, str):
            raise ValueError("file must be of type File or str")

        if self.stderr is not None:
            raise DuplicateError("stderr has already been set to a file")

        if isinstance(file, str):
            file = File(file)

        self.add_outputs(file)
        self.stderr = file

        return self

    def get_stderr(self):
        """Get the :py:class:`~Pegasus.dax4.replica_catalog.File` being used for stderr
        
        :return: the stderr file 
        :rtype: File
        """
        return self.stderr

    def __json__(self):
        return _filter_out_nones(
            {
                "id": self._id,
                "stdin": self.stdin.__json__() if self.stdin is not None else None,
                "stdout": self.stdout.__json__() if self.stdout is not None else None,
                "stderr": self.stderr.__json__() if self.stderr is not None else None,
                "nodeLabel": self.node_label,
                "arguments": [
                    {"lfn": arg.lfn} if isinstance(arg, File) else arg
                    for arg in self.args
                ],
                "uses": [io.__json__() for io in self.uses],
                "profiles": dict(self.profiles) if len(self.profiles) > 0 else None,
                "metadata": self.metadata if len(self.metadata) > 0 else None,
                "hooks": {
                    hook_name: [hook.__json__() for hook in values]
                    for hook_name, values in self.hooks.items()
                }
                if len(self.hooks) > 0
                else None,
            }
        )


class Job(AbstractJob):
    """A typical workflow Job that executes a :py:class:`~Pegasus.dax4.transformation_catalog.Transformation`.

    .. code-block:: python

        # Example
        preprocess = (Transformation("preprocess")
                        .add_metadata("size", 2048)
                        .add_site("test-cluster", "/usr/bin/keg", TransformationType.INSTALLED))

        if1 = File("if1")
        if2 = File("if2")

        of1 = File("of1")
        of2 = File("of2")

        job = (Job(preprocess)
                .add_args("-i", if1, if2, "-o", of1, of2)
                .add_inputs(if1, if2)
                .add_outputs(of1, of2, stage_out=True, register_replica=False))

    """

    def __init__(
        self, transformation, _id=None, node_label=None, namespace=None, version=None,
    ):
        """        
        :param transformation: :py:class:`~Pegasus.dax4.transformation_catalog.Transformation` object or name of the transformation that this job uses
        :type transformation: Transformation or str
        :param _id: a unique id; if none is given then one will be assigned when the job is added by a :py:class:`~Pegasus.dax4.workflow.Workflow`, defaults to None
        :type _id: str, optional
        :param node_label: a brief job description, defaults to None
        :type node_label: str, optional
        :param namespace: namespace to which the :py:class:`~Pegasus.dax4.transformation_catalog.Transformation` belongs, defaults to None
        :type namespace: str, optional
        :param version: version of the given :py:class:`~Pegasus.dax4.transformation_catalog.Transformation`, defaults to None
        :type version: str, optional
        :raises ValueError: transformation must be one of :py:class:`~Pegasus.dax4.transformation_catalog.Transformation` or str
        """
        if isinstance(transformation, Transformation):
            self.transformation = transformation.name
            self.namespace = transformation.namespace
            self.version = transformation.version
        elif isinstance(transformation, str):
            self.transformation = transformation
            self.namespace = namespace
            self.version = version
        else:
            raise ValueError("transformation must be of type Transformation or str")

        AbstractJob.__init__(self, _id=_id, node_label=node_label)

    def __json__(self):
        job_json = {
            "type": "job",
            "namespace": self.namespace,
            "version": self.version,
            "name": self.transformation,
        }

        job_json.update(AbstractJob.__json__(self))

        return _filter_out_nones(job_json)


class DAX(AbstractJob):
    """Job that represents a sub-DAX that will be planned and executed
    by the workflow"""

    def __init__(self, file, _id=None, node_label=None):
        """
        :param file: :py:class:`~Pegasus.dax4.replica_catalog.File` object or name of the dax file that will be used for this job
        :type file: File or str
        :param _id: a unique id; if none is given then one will be assigned when the job is added by a :py:class:`~Pegasus.dax4.workflow.Workflow`, defaults to None
        :type _id: str, optional
        :param node_label: a brief job description, defaults to None
        :type node_label: str, optional
        :raises ValueError: file must be of type :py:class:`~Pegasus.dax4.replica_catalog.File` or str
        """
        AbstractJob.__init__(self, _id=_id, node_label=node_label)

        if not isinstance(file, File) and not isinstance(file, str):
            raise ValueError("file must be of type File or str")

        if isinstance(file, File):
            self.file = file
        else:
            self.file = File(file)

        self.add_inputs(self.file)

    def __json__(self):
        dax_json = {"type": "dax", "file": self.file.lfn}
        dax_json.update(AbstractJob.__json__(self))

        return dax_json


class DAG(AbstractJob):
    """Job represents a sub-DAG that will be executed by this 
    workflow"""

    def __init__(self, file, _id=None, node_label=None):
        """Constructor
        
        :param file: :py:class:`~Pegasus.dax4.replica_catalog.File` object or name of the dag file that will be used for this job
        :type file: File or str
        :param _id: a unique id; if none is given then one will be assigned when the job is added by a :py:class:`~Pegasus.dax4.workflow.Workflow`, defaults to None
        :type _id: str, optional
        :param node_label: a brief job description, defaults to None
        :type node_label: str, optional
        :raises ValueError: file must be of type :py:class:`~Pegasus.dax4.replica_catalog.File` or str
        """
        AbstractJob.__init__(self, _id=_id, node_label=node_label)

        if not isinstance(file, File) and not isinstance(file, str):
            raise ValueError("file must be of type File or str")

        if isinstance(file, File):
            self.file = file
        else:
            self.file = File(file)

        self.add_inputs(self.file)

    def __json__(self):
        dag_json = {"type": "dag", "file": self.file.lfn}
        dag_json.update(AbstractJob.__json__(self))

        return dag_json


class _LinkType(Enum):
    """Internal class defining link types"""

    INPUT = "input"
    OUTPUT = "output"
    CHECKPOINT = "checkpoint"


class _Use:
    """Internal class used to represent input and output files of a job"""

    def __init__(self, file, link_type, stage_out=True, register_replica=True):
        if not isinstance(file, File):
            raise ValueError("file must be one of type File")

        self.file = file

        if not isinstance(link_type, _LinkType):
            raise ValueError("link_type must be one of _LinkType")

        self._type = link_type.value

        self.stage_out = stage_out
        self.register_replica = register_replica

    def __hash__(self):
        return hash(self.file)

    def __eq__(self, other):
        if isinstance(other, _Use):
            return self.file.lfn == other.file.lfn
        raise ValueError("_Use cannot be compared with {0}".format(type(other)))

    def __json__(self):
        return {
            "file": self.file.__json__(),
            "type": self._type,
            "stageOut": self.stage_out,
            "registerReplica": self.register_replica,
        }


class _JobDependency:
    """Internal class used to represent a jobs dependencies within a workflow"""

    def __init__(self, parent_id, children_ids):
        self.parent_id = parent_id
        self.children_ids = children_ids

    def __eq__(self, other):
        if isinstance(other, _JobDependency):
            return (
                self.parent_id == other.parent_id
                and self.children_ids == other.children_ids
            )
        raise ValueError(
            "_JobDependency cannot be compared with {0}".format(type(other))
        )

    def __json__(self):
        return {"id": self.parent_id, "children": list(self.children_ids)}


class Workflow(Writable, HookMixin, ProfileMixin, MetadataMixin):
    """Represents multi-step computational steps as a directed
    acyclic graph.
    
    .. code-block:: python

        # Example
        from Pegasus.dax4 import *

        # --- replicas -----------------------------------------------------------------
        rc = ReplicaCatalog()
        fa = File("f.a").add_metadata("SIZE", "1024")

        rc.add_replica(
            fa,
            "file:///lfs/voeckler/src/svn/pegasus/trunk/examples/grid-blackdiamond-perl/f.a",
            "local",
        )

        # --- transformations ----------------------------------------------------------
        tc = TransformationCatalog()

        preprocess = (Transformation("preprocess", namespace="diamond", version="2.0")
                        .add_profile(Namespace.GLOBUS, "maxtime", 2)
                        .add_profile(Namespace.DAGMAN, "retry", 3)
                        .add_site("local", "file:///opt/pegasus/latest/bin/keg", TransformationType.STAGEABLE, arch=Arch.X86_64, ostype=OSType.LINUX) 
                        .add_site_profile("local", Namespace.ENV, "JAVA_HOME", "/path")
                        .add_shell_hook(EventType.START, "/bin/echo 'hello i started'")
                        .add_shell_hook(EventType.END, "/bin/echo 'hello i ended'")
                        .add_metadata("metadata_key", "metadata_value")
                        .add_metadata("metadata_key2", "metadata_value2"))

        analyze = (Transformation("analyze", namespace="diamond", version="2.0")
                    .add_profile(Namespace.GLOBUS, "maxtime", 2)
                    .add_profile(Namespace.DAGMAN, "retry", 3)
                    .add_site("local", "file:///opt/pegasus/latest/bin/keg", TransformationType.STAGEABLE, arch=Arch.X86_64, ostype=OSType.LINUX))

        findrange = (Transformation("findrange", namespace="diamond", version="2.0")
                        .add_profile(Namespace.GLOBUS, "maxtime", 2)
                        .add_profile(Namespace.DAGMAN, "retry", 3)
                        .add_site("local", "file:///opt/pegasus/latest/bin/keg", TransformationType.STAGEABLE, arch=Arch.X86_64, ostype=OSType.LINUX))

        tc.add_transformations(preprocess, analyze, findrange)

        # --- workflow -----------------------------------------------------------------
        wf = Workflow("black-diamond", infer_dependencies=True)

        (wf.add_profile(Namespace.ENV, "WORKFLOW_ENV", "something")
            .add_shell_hook(EventType.START, "/bin/echo 123")
            .add_metadata("WORKFLOW_AUTHOR", "GIDEON"))

        fb1 = File("f.b1").add_metadata("SIZE", "1024")
        fb2 = File("f.b2").add_metadata("SIZE", "2048")
        wf.add_jobs(Job(preprocess, _id="pre")
                    .add_args("-a", "preprocess", "-T60", "-i", fa, "-o", fb1, fb2)
                    .add_inputs(fa)
                    .add_outputs(fb1, fb2, stage_out=True, register_replica=False)
                    .add_profile(Namespace.ENV, "ENV", "234")
                    .add_shell_hook(EventType.START, "/bin/echo 'hello i started'")
                    .add_shell_hook(EventType.END, "/bin/echo 'hello i ended'")
                    .add_metadata("metadata_key", "metadata_value")
                    .add_metadata("metadat_key2", "metadata_value2"))

        fc1 = File("f.c1")
        wf.add_jobs(Job(findrange, _id="fr1")
                    .add_args("-a", "findrange", "-T60", "-i", fb1, "-o", fc1)
                    .add_inputs(fb1)
                    .add_outputs(fc1))

        fc2 = File("f.c2")
        wf.add_jobs(Job(findrange, _id="fr2")
                    .add_args("-a", "findrange", "-T60", "-i", fb2, "-o", fc2)
                    .add_inputs(fb2)
                    .add_outputs(fc2))

        fd = File("f.d")
        wf.add_jobs(Job(analyze, _id="analyze")
                        .add_args("-a", "analyze", "-T60", "-i", fc1, fc2, "-o", fd)
                        .add_inputs(fc1, fc2)
                        .add_outputs(fd)) 

        (wf.include_catalog(rc)
            .include_catalog(tc))

        wf.write(non_default_filepath="workflow_with_catalogs.yml", file_format=FileFormat.YAML)
    
    """

    def __init__(self, name, infer_dependencies=False):
        """
        :param name: name of the :py:class:`~Pegasus.dax4.workflow.Workflow`
        :type name: str
        :param infer_dependencies: whether or not to automatically compute job dependencies based on input and output files used by each job, defaults to False
        :type infer_dependencies: bool, optional
        """
        self.name = name
        self.infer_dependencies = infer_dependencies

        self.jobs = dict()
        self.dependencies = defaultdict(_JobDependency)

        # sequence unique to this workflow only
        self.sequence = 1

        self.site_catalog = None
        self.transformation_catalog = None
        self.replica_catalog = None

        self.hooks = defaultdict(list)
        self.profiles = defaultdict(dict)
        self.metadata = dict()

    def add_jobs(self, *jobs):
        """Add one or more jobs at a time to the Workflow
        
        :raises DuplicateError: a job with the same id already exists in this workflow 
        :return: self
        """
        for job in jobs:
            if job._id == None:
                job._id = self._get_next_job_id()

            if job._id in self.jobs:
                raise DuplicateError("Job with id {0} already exists".format(job._id))

            self.jobs[job._id] = job

        return self

    def get_job(self, _id):
        """Retrieve the job with the given id
        
        :param _id: id of the job to be retrieved from the Workflow
        :type _id: str
        :raises NotFoundError: a job with the given id does not exist in this workflow
        :return: the job with the given id
        :rtype: Job
        """
        if _id not in self.jobs:
            raise NotFoundError("job with _id={0} not found".format(_id))

        return self.jobs[_id]

    def _get_next_job_id(self):
        """Get the next job id from a sequence specific to this workflow
        
        :return: a new unique job id
        :rtype: str
        """
        next_id = None
        while not next_id or next_id in self.jobs:
            next_id = "ID{:07d}".format(self.sequence)
            self.sequence += 1

        return next_id

    def include_catalog(self, catalog):
        """Inline any of :py:class:`~Pegasus.dax4.replica_catalog.ReplicaCatalog`, 
        :py:class:`~Pegasus.dax4.transformation_catalog.TransformationCatalog`, or
        :py:class:`~Pegasus.dax4.site_catalog.SiteCatalog` into this workflow. When a workflow is written 
        to a file, if a catalog has been included, then the contents of the catalog
        will appear on the same file as the workflow. 
        
        :param catalog: the catalog to be included
        :type catalog: SiteCatalog or TransformationCatalog or ReplicaCatalog
        :raises ValueError: a :py:class:`~Pegasus.dax4.replica_catalog.ReplicaCatalog` has already been included
        :raises ValueError: a :py:class:`~Pegasus.dax4.transformation_catalog.TransformationCatalog` has already been included
        :raises ValueError: a :py:class:`~Pegasus.dax4.site_catalog.SiteCatalog` has already been included
        :raises ValueError: invalid catalog was given
        :return: self
        """
        if isinstance(catalog, ReplicaCatalog):
            if self.replica_catalog is not None:
                raise ValueError(
                    "a ReplicaCatalog has already been inlined in this Workflow"
                )

            self.replica_catalog = catalog

        elif isinstance(catalog, TransformationCatalog):
            if self.transformation_catalog is not None:
                raise ValueError(
                    "a TransformationCatalog has already been inlined in this Workflow"
                )

            self.transformation_catalog = catalog

        elif isinstance(catalog, SiteCatalog):
            if self.site_catalog is not None:
                raise ValueError(
                    "a SiteCatalog has already been inlined in this Workflow"
                )

            self.site_catalog = catalog

        else:
            raise ValueError("{0} cannot be included in this Workflow".format(catalog))

        return self

    def add_dependency(self, parent, *children):
        """Manually specify a dependency between one job to one or more other jobs
        
        :param parent: parent job
        :type parent: AbstractJob
        :param children: one or more child jobs
        :raises DuplicateError: a dependency has already been added between the parent job and one of the child jobs
        :return: self
        """
        children_ids = {child._id for child in children}
        parent_id = parent._id
        if parent_id in self.dependencies:
            if not self.dependencies[parent_id].children_ids.isdisjoint(children_ids):
                raise DuplicateError(
                    "A dependency already exists between parentid: {0} and children_ids: {1}".format(
                        parent_id, children_ids
                    )
                )

            self.dependencies[parent_id].children_ids.update(children_ids)
        else:
            self.dependencies[parent_id] = _JobDependency(parent_id, children_ids)

        return self

    def _infer_dependencies(self):
        """Internal function for automatically computing dependencies based on
        Job input and output files. This is called when Workflow.infer_dependencies is
        set to True. 
        """

        if self.infer_dependencies:
            mapping = dict()

            """
            create a mapping:
            {
                <filename>: (set(), set())
            }

            where mapping[filename][0] are jobs that use this file as input
            and mapping[filename][1] are jobs that use this file as output
            """
            for _id, job in self.jobs.items():
                if job.stdin:
                    if job.stdin.lfn not in mapping:
                        mapping[job.stdin.lfn] = (set(), set())

                    mapping[job.stdin.lfn][0].add(job)

                if job.stdout:
                    if job.stdout.lfn not in mapping:
                        mapping[job.stdout.lfn] = (set(), set())

                    mapping[job.stdout.lfn][1].add(job)

                if job.stderr:
                    if job.stderr.lfn not in mapping:
                        mapping[job.stderr.lfn][1].add(job)

                """
                for _input in job.inputs:
                    if _input.file.lfn not in mapping:
                        mapping[_input.file.lfn] = (set(), set())

                    mapping[_input.file.lfn][0].add(job)

                for output in job.outputs:
                    if output.file.lfn not in mapping:
                        mapping[output.file.lfn] = (set(), set())

                    mapping[output.file.lfn][1].add(job)
                """
                for io in job.uses:
                    if io.file.lfn not in mapping:
                        mapping[io.file.lfn] = (set(), set())

                    if io._type == _LinkType.INPUT.value:
                        mapping[io.file.lfn][0].add(job)
                    elif io._type == _LinkType.OUTPUT.value:
                        mapping[io.file.lfn][1].add(job)

            """    
            Go through the mapping and for each file add dependencies between the
            job producing a file and the jobs consuming the file
            """
            for _, io in mapping.items():
                inputs = io[0]

                if len(io[1]) > 0:
                    # only a single job should produce this file
                    output = io[1].pop()

                    for _input in inputs:
                        try:
                            self.add_dependency(output, _input)
                        except DuplicateError:
                            pass

    def write(self, non_default_filepath="", file_format=FileFormat.YAML):
        """Write this catalog, formatted in YAML, to a file
        
        :param filepath: path to which this catalog will be written, defaults to self.filepath if filepath is "" or None
        :type filepath: str, optional
        """
        self._infer_dependencies()
        Writable.write(
            self, non_default_filepath=non_default_filepath, file_format=file_format
        )

    def __json__(self):
        # remove 'pegasus' from tc, rc, sc as it is not needed when they
        # are included in the Workflow which already contains 'pegasus'
        rc = None
        if self.replica_catalog is not None:
            rc = self.replica_catalog.__json__()
            del rc["pegasus"]

        tc = None
        if self.transformation_catalog is not None:
            tc = self.transformation_catalog.__json__()
            del tc["pegasus"]

        sc = None
        if self.site_catalog is not None:
            sc = self.site_catalog.__json__()
            del sc["pegasus"]

        return _filter_out_nones(
            {
                "pegasus": PEGASUS_VERSION,
                "name": self.name,
                "replicaCatalog": rc,
                "transformationCatalog": tc,
                "siteCatalog": sc,
                "jobs": [job.__json__() for _id, job in self.jobs.items()],
                "jobDependencies": [
                    dependency.__json__()
                    for _id, dependency in self.dependencies.items()
                ]
                if len(self.dependencies) > 0
                else None,
                "profiles": dict(self.profiles) if len(self.profiles) > 0 else None,
                "metadata": self.metadata if len(self.metadata) > 0 else None,
                "hooks": {
                    hook_name: [hook.__json__() for hook in values]
                    for hook_name, values in self.hooks.items()
                }
                if len(self.hooks) > 0
                else None,
            }
        )